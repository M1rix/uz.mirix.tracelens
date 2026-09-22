package uz.mirix.tracelens;

import uz.mirix.tracelens.internal.TraceContext;
import uz.mirix.tracelens.internal.TraceContextHolder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/** Minimal programmatic API for adding application-specific spans. */
public final class TraceLens {
    private static final TraceScope NOOP = () -> { };
    private TraceLens() { }
    public static Optional<String> currentTraceId() { TraceContext c=TraceContextHolder.current(); return c==null?Optional.empty():Optional.of(c.traceId()); }
    public static TraceScope span(String name) { return span(name, Map.of()); }
    public static TraceScope span(String name, Map<String,String> attributes) {
        TraceContext c=TraceContextHolder.current(); if(c==null) return NOOP; long started=System.nanoTime();
        return () -> c.record(TraceCategory.CUSTOM,name,started,System.nanoTime(),attributes);
    }
    public static void trace(String name,Runnable operation){ try(TraceScope ignored=span(name)){ operation.run(); } }
    public static <T> T trace(String name,Callable<T> operation) throws Exception { try(TraceScope ignored=span(name)){ return operation.call(); } }
}
