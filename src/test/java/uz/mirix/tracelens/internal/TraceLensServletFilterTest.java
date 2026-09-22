package uz.mirix.tracelens.internal;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uz.mirix.tracelens.TraceCategory;
import uz.mirix.tracelens.TraceLensProperties;
import uz.mirix.tracelens.TraceReport;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceLensServletFilterTest {

    @Test
    void publishesRouteTemplateAndPreservesExistingServerTiming() throws Exception {
        TraceLensProperties properties = new TraceLensProperties();
        AtomicReference<TraceReport> captured = new AtomicReference<>();
        TraceLifecycle lifecycle = new TraceLifecycle(properties, List.of(captured::set));
        TraceLensServletFilter filter = new TraceLensServletFilter(properties, lifecycle);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/42");
        request.setAttribute(
            "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern",
            "/api/orders/{id}"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.addHeader("Server-Timing", "application;dur=1");

        filter.doFilter(request, response, (req, res) -> res.getWriter().write("{\"ok\":true}"));

        assertThat(response.getHeaders("Server-Timing"))
            .contains("application;dur=1")
            .anyMatch(value -> value.contains("total;dur="));
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().route()).isEqualTo("/api/orders/{id}");
        assertThat(captured.get().status()).isEqualTo(200);
        assertThat(captured.get().spans())
            .anyMatch(span -> span.category() == TraceCategory.SERIALIZATION);
    }

    @Test
    void stripsServletContextPathFromFallbackRoute() throws Exception {
        TraceLensProperties properties = new TraceLensProperties();
        AtomicReference<TraceReport> captured = new AtomicReference<>();
        TraceLifecycle lifecycle = new TraceLifecycle(properties, List.of(captured::set));
        TraceLensServletFilter filter = new TraceLensServletFilter(properties, lifecycle);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/shop/api/orders");
        request.setContextPath("/shop");
        request.setRequestURI("/shop/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(captured.get().route()).isEqualTo("/api/orders");
    }

    @Test
    void reportsUnhandledServletFailureAs500() {
        TraceLensProperties properties = new TraceLensProperties();
        AtomicReference<TraceReport> captured = new AtomicReference<>();
        TraceLifecycle lifecycle = new TraceLifecycle(properties, List.of(captured::set));
        TraceLensServletFilter filter = new TraceLensServletFilter(properties, lifecycle);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/boom");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() ->
            filter.doFilter(request, response, (req, res) -> {
                throw new ServletException("boom");
            })
        ).isInstanceOf(ServletException.class);

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().status()).isEqualTo(500);
        assertThat(captured.get().errorType()).isEqualTo(ServletException.class.getName());
    }
}
