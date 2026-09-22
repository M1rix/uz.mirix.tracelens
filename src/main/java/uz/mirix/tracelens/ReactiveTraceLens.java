package uz.mirix.tracelens;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.mirix.tracelens.internal.TraceContext;
import uz.mirix.tracelens.internal.TraceContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Programmatic TraceLens API for reactive pipelines.
 *
 * <p>Unlike the imperative {@link TraceLens} API, this class reads the active
 * request trace from Reactor Context and therefore remains correct across
 * thread switches.</p>
 */
public final class ReactiveTraceLens {

    private ReactiveTraceLens() {
    }

    public static Mono<Optional<String>> currentTraceId() {
        return Mono.deferContextual(view -> {
            TraceContext context = view.getOrDefault(TraceContextHolder.REACTOR_KEY, null);
            return Mono.just(context == null ? Optional.empty() : Optional.of(context.traceId()));
        });
    }

    public static <T> Mono<T> trace(String name, Mono<T> source) {
        return trace(name, Map.of(), source);
    }

    public static <T> Mono<T> trace(String name, Map<String, String> attributes, Mono<T> source) {
        Objects.requireNonNull(source, "source");
        Map<String, String> safeAttributes = attributes == null ? Map.of() : Map.copyOf(attributes);

        return Mono.deferContextual(view -> {
            TraceContext context = view.getOrDefault(TraceContextHolder.REACTOR_KEY, null);
            if (context == null) {
                return source;
            }

            long startedNanos = System.nanoTime();
            return source.doFinally(signal -> context.record(
                TraceCategory.CUSTOM,
                name,
                startedNanos,
                System.nanoTime(),
                withSignal(safeAttributes, signal.name())
            ));
        });
    }

    public static <T> Flux<T> trace(String name, Flux<T> source) {
        return trace(name, Map.of(), source);
    }

    public static <T> Flux<T> trace(String name, Map<String, String> attributes, Flux<T> source) {
        Objects.requireNonNull(source, "source");
        Map<String, String> safeAttributes = attributes == null ? Map.of() : Map.copyOf(attributes);

        return Flux.deferContextual(view -> {
            TraceContext context = view.getOrDefault(TraceContextHolder.REACTOR_KEY, null);
            if (context == null) {
                return source;
            }

            long startedNanos = System.nanoTime();
            return source.doFinally(signal -> context.record(
                TraceCategory.CUSTOM,
                name,
                startedNanos,
                System.nanoTime(),
                withSignal(safeAttributes, signal.name())
            ));
        });
    }

    private static Map<String, String> withSignal(Map<String, String> attributes, String signal) {
        Map<String, String> result = new HashMap<>(attributes);
        result.put("signal", signal);
        return result;
    }
}
