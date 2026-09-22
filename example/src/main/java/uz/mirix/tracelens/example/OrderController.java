package uz.mirix.tracelens.example;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@RestController
@RequestMapping("/demo/orders")
public class OrderController {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;

    public OrderController(JdbcTemplate jdbcTemplate, RestClient.Builder restClientBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.restClient = restClientBuilder.build();
    }

    @GetMapping("/{id}")
    public OrderResponse order(@PathVariable long id, HttpServletRequest request) {
        OrderRow order = jdbcTemplate.queryForObject(
            """
            select id, customer, total
            from demo_orders
            where id = ?
            """,
            (resultSet, rowNum) -> new OrderRow(
                resultSet.getLong("id"),
                resultSet.getString("customer"),
                resultSet.getBigDecimal("total")
            ),
            id
        );

        if (order == null) {
            throw new IllegalStateException("Demo order was not found");
        }

        InventoryResponse inventory = restClient.get()
            .uri(baseUrl(request) + "/inventory/" + id)
            .retrieve()
            .body(InventoryResponse.class);

        if (inventory == null) {
            throw new IllegalStateException("Inventory response was empty");
        }

        return new OrderResponse(order.id(), order.customer(), order.total(), inventory.available());
    }

    private static String baseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }

    private record OrderRow(long id, String customer, BigDecimal total) {
    }

    public record OrderResponse(long id, String customer, BigDecimal total, int available) {
    }

    public record InventoryResponse(long orderId, int available) {
    }
}
