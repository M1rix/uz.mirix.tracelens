package uz.mirix.tracelens;

import java.time.Duration;
import java.util.Map;

/** Immutable timed operation captured inside one request. */
public record TraceSpan(TraceCategory category,String name,long startOffsetNanos,long durationNanos,Map<String,String> attributes){
    public TraceSpan{attributes=attributes==null?Map.of():Map.copyOf(attributes);} public Duration duration(){return Duration.ofNanos(durationNanos);} public double durationMillis(){return durationNanos/1_000_000.0d;}
}
