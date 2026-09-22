package uz.mirix.tracelens.internal;

import uz.mirix.tracelens.TraceLensProperties;
import uz.mirix.tracelens.TraceReport;
import uz.mirix.tracelens.TraceSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServerTimingFormatter {

    private ServerTimingFormatter() {
    }

    public static String format(TraceReport report, TraceLensProperties properties) {
        int limit = Math.min(
            properties.getServerTiming().getMaxMetrics(),
            report.spans().size() + 1
        );
        List<String> metrics = new ArrayList<>(limit);
        AtomicInteger sequence = new AtomicInteger();

        for (TraceSpan span : report.spans()) {
            if (metrics.size() >= limit - 1) {
                break;
            }

            String token = span.category().name().toLowerCase(Locale.ROOT)
                + "-"
                + sequence.incrementAndGet();

            StringBuilder metric = new StringBuilder(token)
                .append(";dur=")
                .append(millis(span.durationNanos()));

            if (properties.getServerTiming().isIncludeDescriptions()) {
                String description = SafeText.singleLine(
                    span.name().replace("\\", "\\\\").replace("\"", "\\\""),
                    properties.getServerTiming().getMaxDescriptionLength()
                );
                metric.append(";desc=\"").append(description).append('\"');
            }
            metrics.add(metric.toString());
        }

        metrics.add("total;dur=" + millis(report.durationNanos()) + ";desc=\"TraceLens total\"");
        return String.join(", ", metrics);
    }

    private static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0d);
    }
}
