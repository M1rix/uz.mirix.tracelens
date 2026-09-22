package uz.mirix.tracelens.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {

    @GetMapping("/inventory/{orderId}")
    public OrderController.InventoryResponse inventory(@PathVariable long orderId) {
        return new OrderController.InventoryResponse(orderId, 7);
    }
}
