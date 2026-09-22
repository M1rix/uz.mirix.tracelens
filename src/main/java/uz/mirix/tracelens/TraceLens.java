package uz.mirix.tracelens;

import uz.mirix.tracelens.internal.TraceContext;
import uz.mirix.tracelens.internal.TraceContextHolder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Minimal programmatic API for imperative custom spans.
 *
 * <p>For WebFlux/reactive chains use {@link ReactiveTraceLens}; a ThreadLocal
 * cannot correctly represent Reactor context across thread switches.</p>
 */
public final class TraceLens {

    private static final TraceScope NOOP = () -> { };

    private TraceLens() {
    }

    public static Optional<String> currentTraceId() {
        TraceContext context = TraceContextHolder.current();
        return context == null ? Optional.empty() : Optional.of(context.traceId());
    }

    public static TraceScope span(String name) {
        return span(name, Map.of());
    }

    public static TraceScope span(String name, Map<String, String> attributes) {
        TraceContext context = TraceContextHolder.current();
        if (context == null) {
            return NOOP;
        }

        long startedNanos = System.nanoTime();
        Map<String, String> safeAttributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        return () -> context.record(
            TraceCategory.CUSTOM,
            name,
            startedNanos,
            System.nanoTime(),
            safeAttributes
        );
    }

    public static void trace(String name, Runnable operation) {
        try (TraceScope ignored = span(name)) {
            operation.run();
        }
    }

    public static <T> T trace(String name, Callable<T> operation) throws Exception {
        try (TraceScope ignored = span(name)) {
            return operation.call();
        }
    }
}
