package roomescape.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import roomescape.order.client.TossPaymentClient;
import roomescape.order.domain.Order;
import roomescape.order.exception.OrderNotFoundException;
import roomescape.order.exception.PaymentAmountMismatchException;
import roomescape.order.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TossPaymentClient tossPaymentClient;

    public Order makeOrder(Long amount) {
        return orderRepository.save(Order.of(amount));
    }

    public void confirm(String orderId, String paymentKey, Long amount) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getAmount().equals(amount)) {
            throw new PaymentAmountMismatchException();
        }

        tossPaymentClient.confirm(paymentKey, orderId, amount);
        orderRepository.confirmByOrderId(orderId, paymentKey);
    }
}
