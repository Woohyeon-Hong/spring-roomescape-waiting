package roomescape.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.order.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

//    @PostMapping("/{orderId}/confirm")
//    public ResponseEntity<Void> confirmOrder(
//            @PathVariable String orderId,
//            @Valid @RequestBody PaymentConfirmRequest request
//    ) {
//        orderService.confirm(orderId, request.paymentKey(), request.amount());
//        return ResponseEntity.noContent().build();
//    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable("orderId") String orderId) {
        orderService.deleteOrderByOrderId(orderId);
        return ResponseEntity.noContent().build();
    }
}
