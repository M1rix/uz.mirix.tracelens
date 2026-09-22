package uz.mirix.tracelens;
import java.time.Duration; import java.time.Instant; import java.util.List;
/** Immutable completed HTTP request trace. */
public record TraceReport(String traceId,Instant startedAt,String method,String route,int status,long durationNanos,List<TraceSpan> spans,String errorType,boolean truncated){ public TraceReport{spans=List.copyOf(spans);} public Duration duration(){return Duration.ofNanos(durationNanos);} public double durationMillis(){return durationNanos/1_000_000.0d;} }
