package roomescape.order.dto;

import roomescape.order.domain.Order;

public record OrderResponse(
        String orderId,
        Long amount,
        String paymentKey
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getAmount(),
                order.getPaymentKey()
        );
    }
}
