package uz.mirix.tracelens.internal;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.mirix.tracelens.TraceCategory;
import uz.mirix.tracelens.TraceLensProperties;
import uz.mirix.tracelens.TraceReport;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class TraceLensServletFilter extends OncePerRequestFilter {

    private static final String BEST_MATCHING_PATTERN_ATTRIBUTE =
        "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern";

    private final TraceLensProperties properties;
    private final TraceLifecycle lifecycle;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TraceLensServletFilter(TraceLensProperties properties, TraceLifecycle lifecycle) {
        this.properties = properties;
        this.lifecycle = lifecycle;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathWithinApplication(request);
        return properties.getWeb().getExclude().stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        if (!lifecycle.shouldTrace()) {
            chain.doFilter(request, response);
            return;
        }

        TraceContext context = lifecycle.start(request.getMethod(), pathWithinApplication(request));
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicBoolean serverTimingWritten = new AtomicBoolean();
        TraceLensHttpServletResponseWrapper wrapped = new TraceLensHttpServletResponseWrapper(response);

        wrapped.beforeFirstWrite(() -> addServerTimingIfPossible(
            context,
            request,
            response,
            failure.get(),
            System.nanoTime(),
            serverTimingWritten
        ));

        TraceContextHolder.bind(context);
        try {
            chain.doFilter(request, wrapped);
        } catch (ServletException | IOException | RuntimeException | Error ex) {
            failure.compareAndSet(null, ex);
            throw ex;
        } finally {
            TraceContextHolder.clear();

            if (request.isAsyncStarted()) {
                registerAsyncCompletion(
                    request,
                    response,
                    wrapped,
                    context,
                    failure,
                    completed,
                    serverTimingWritten
                );
            } else {
                complete(
                    request,
                    response,
                    wrapped,
                    context,
                    failure.get(),
                    completed,
                    serverTimingWritten
                );
            }
        }
    }

    private void registerAsyncCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        TraceLensHttpServletResponseWrapper wrapped,
        TraceContext context,
        AtomicReference<Throwable> failure,
        AtomicBoolean completed,
        AtomicBoolean serverTimingWritten
    ) {
        AsyncListener listener = new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                complete(
                    request,
                    response,
                    wrapped,
                    context,
                    failure.get(),
                    completed,
                    serverTimingWritten
                );
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                failure.compareAndSet(null, new TimeoutException("Servlet async request timed out"));
            }

            @Override
            public void onError(AsyncEvent event) {
                failure.compareAndSet(null, event.getThrowable());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                event.getAsyncContext().addListener(this);
            }
        };

        try {
            request.getAsyncContext().addListener(listener);
        } catch (IllegalStateException alreadyCompleted) {
            complete(
                request,
                response,
                wrapped,
                context,
                failure.get(),
                completed,
                serverTimingWritten
            );
        }
    }

    private void complete(
        HttpServletRequest request,
        HttpServletResponse response,
        TraceLensHttpServletResponseWrapper wrapped,
        TraceContext context,
        Throwable failure,
        AtomicBoolean completed,
        AtomicBoolean serverTimingWritten
    ) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        long endedNanos = System.nanoTime();
        long firstWriteNanos = wrapped.firstWriteNanos();
        if (firstWriteNanos > 0 && endedNanos >= firstWriteNanos) {
            context.record(
                TraceCategory.SERIALIZATION,
                "serialization / response write",
                firstWriteNanos,
                endedNanos,
                Map.of()
            );
        }

        TraceReport report = context.finish(
            route(request),
            effectiveStatus(response, failure),
            failure,
            endedNanos
        );

        if (!serverTimingWritten.get()) {
            addServerTimingIfPossible(
                context,
                request,
                response,
                failure,
                endedNanos,
                serverTimingWritten
            );
        }
        lifecycle.publish(report);
    }

    private void addServerTimingIfPossible(
        TraceContext context,
        HttpServletRequest request,
        HttpServletResponse response,
        Throwable failure,
        long nowNanos,
        AtomicBoolean serverTimingWritten
    ) {
        if (!properties.getServerTiming().isEnabled()
            || response.isCommitted()
            || !serverTimingWritten.compareAndSet(false, true)) {
            return;
        }

        TraceReport partial = context.finish(
            route(request),
            effectiveStatus(response, failure),
            failure,
            nowNanos
        );
        response.addHeader("Server-Timing", ServerTimingFormatter.format(partial, properties));
    }

    private static int effectiveStatus(HttpServletResponse response, Throwable failure) {
        int status = response.getStatus();
        return failure != null && status < 400 ? 500 : status;
    }

    private static String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? pathWithinApplication(request) : pattern.toString();
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null
            && !contextPath.isEmpty()
            && uri.startsWith(contextPath)
            && uri.length() >= contextPath.length()) {
            String path = uri.substring(contextPath.length());
            return path.isEmpty() ? "/" : path;
        }
        return uri;
    }
}
