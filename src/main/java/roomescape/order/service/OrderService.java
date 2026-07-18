package roomescape.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import roomescape.order.domain.Order;
import roomescape.order.exception.OrderNotFoundException;
import roomescape.order.exception.PaymentAmountMismatchException;
import roomescape.order.repository.OrderRepository;
import roomescape.payment.PaymentConfirmation;
import roomescape.payment.PaymentGateway;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public Order makeOrder(Long amount) {
        return orderRepository.save(Order.of(amount));
    }

    public void confirm(String orderId, String paymentKey, Long amount) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getAmount().equals(amount)) {
            throw new PaymentAmountMismatchException();
        }

        paymentGateway.confirm(
                new PaymentConfirmation(paymentKey, orderId, amount)
        );

        orderRepository.confirmByOrderId(orderId, paymentKey);
    }
}
