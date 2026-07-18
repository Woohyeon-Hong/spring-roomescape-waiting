package roomescape.order.controller.dto;

import roomescape.order.domain.Order;

public record OrderResponse(
        String orderId,
        Long amount
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getOrderId(), order.getAmount());
    }
}
