package uz.mirix.tracelens.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TraceLensConsumerSmokeTest {

    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder restClientBuilder;

    @Test
    void capturesRealSqlAndOutboundHttpSpans() {
        RestClient client = restClientBuilder.build();

        ResponseEntity<OrderController.OrderResponse> response = client.get()
            .uri("http://127.0.0.1:" + port + "/demo/orders/42")
            .retrieve()
            .toEntity(OrderController.OrderResponse.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(42L);
        assertThat(response.getBody().customer()).isEqualTo("Mirix Demo");
        assertThat(response.getBody().total()).isEqualByComparingTo(new BigDecimal("125000.00"));
        assertThat(response.getBody().available()).isEqualTo(7);

        List<String> serverTiming = response.getHeaders().get("Server-Timing");
        assertThat(serverTiming).isNotNull().isNotEmpty();

        String timing = String.join(", ", serverTiming);
        assertThat(timing)
            .contains("sql-")
            .contains("http-")
            .contains("total;dur=")
            .contains("SQL SELECT")
            .contains("HTTP GET");
    }
}
