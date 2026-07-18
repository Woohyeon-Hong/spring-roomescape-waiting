package roomescape.order.controller;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.order.controller.dto.OrderRequest;
import roomescape.order.controller.dto.OrderResponse;
import roomescape.order.controller.dto.PaymentConfirmRequest;
import roomescape.order.domain.Order;
import roomescape.order.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.makeOrder(request.amount());
        OrderResponse response = OrderResponse.from(order);

        return ResponseEntity
                .created(URI.create("/orders/" + order.getId()))
                .body(response);
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirmOrder(
            @PathVariable String orderId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        orderService.confirm(orderId, request.paymentKey(), request.amount());
        return ResponseEntity.noContent().build();
    }
}
