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

    // ponytail: 실제 토스 결제 승인(paymentKey 검증) 없이 바로 확정 처리 — 결제 연동 붙을 때 교체
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirmOrder(@PathVariable String orderId) {
        orderService.confirm(orderId);
        return ResponseEntity.noContent().build();
    }
}
