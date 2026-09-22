package uz.mirix.tracelens.internal;

import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import uz.mirix.tracelens.TraceLensProperties;
import uz.mirix.tracelens.TraceReport;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class TraceLensReactiveWebFilter implements WebFilter {

    private final TraceLensProperties properties;
    private final TraceLifecycle lifecycle;

    public TraceLensReactiveWebFilter(TraceLensProperties properties, TraceLifecycle lifecycle) {
        this.properties = properties;
        this.lifecycle = lifecycle;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!lifecycle.shouldTrace() || excluded(path)) {
            return chain.filter(exchange);
        }

        TraceContext context = lifecycle.start(exchange.getRequest().getMethod().name(), path);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean published = new AtomicBoolean();

        if (properties.getServerTiming().isEnabled()) {
            exchange.getResponse().beforeCommit(() -> {
                TraceReport partial = context.finish(
                    route(exchange),
                    status(exchange, failure.get()),
                    failure.get(),
                    System.nanoTime()
                );
                exchange.getResponse().getHeaders().set(
                    "Server-Timing",
                    ServerTimingFormatter.format(partial, properties)
                );
                return Mono.empty();
            });
        }

        return chain.filter(exchange)
            .doOnError(failure::set)
            .doFinally(signal -> {
                if (published.compareAndSet(false, true)) {
                    lifecycle.publish(context.finish(
                        route(exchange),
                        status(exchange, failure.get()),
                        failure.get(),
                        System.nanoTime()
                    ));
                }
            })
            .contextWrite(contextView -> contextView.put(TraceContextHolder.REACTOR_KEY, context));
    }

    private boolean excluded(String path) {
        org.springframework.util.AntPathMatcher matcher = new org.springframework.util.AntPathMatcher();
        return properties.getWeb().getExclude().stream().anyMatch(pattern -> matcher.match(pattern, path));
    }

    private static String route(ServerWebExchange exchange) {
        Object pattern = exchange.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? exchange.getRequest().getPath().value() : pattern.toString();
    }

    private static int status(ServerWebExchange exchange, Throwable failure) {
        int status = exchange.getResponse().getStatusCode() == null
            ? 200
            : exchange.getResponse().getStatusCode().value();
        return failure != null && status < 400 ? 500 : status;
    }
}
