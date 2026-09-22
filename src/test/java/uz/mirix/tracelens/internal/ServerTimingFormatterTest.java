package uz.mirix.tracelens.internal;

import org.junit.jupiter.api.Test;
import uz.mirix.tracelens.TraceCategory;
import uz.mirix.tracelens.TraceLensProperties;
import uz.mirix.tracelens.TraceReport;
import uz.mirix.tracelens.TraceSpan;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServerTimingFormatterTest {

    private static final TraceReport REPORT = new TraceReport(
        "abc",
        Instant.now(),
        "GET",
        "/api/orders/{id}",
        200,
        20_000_000,
        List.of(
            new TraceSpan(
                TraceCategory.SQL,
                "SQL SELECT: select * from secret_orders where id = ?",
                0,
                3_000_000,
                Map.of()
            ),
            new TraceSpan(
                TraceCategory.HTTP,
                "HTTP GET inventory-internal.example",
                4_000_000,
                8_000_000,
                Map.of()
            )
        ),
        null,
        false
    );

    @Test
    void hidesInternalDescriptionsByDefault() {
        String header = ServerTimingFormatter.format(REPORT, new TraceLensProperties());

        assertThat(header)
            .contains("sql-1;dur=3.00")
            .contains("http-2;dur=8.00")
            .contains("total;dur=20.00")
            .doesNotContain("secret_orders")
            .doesNotContain("inventory-internal.example");
    }

    @Test
    void canIncludeDescriptionsExplicitlyForLocalDebugging() {
        TraceLensProperties properties = new TraceLensProperties();
        properties.getServerTiming().setIncludeDescriptions(true);

        String header = ServerTimingFormatter.format(REPORT, properties);

        assertThat(header)
            .contains("secret_orders")
            .contains("inventory-internal.example");
    }
}
