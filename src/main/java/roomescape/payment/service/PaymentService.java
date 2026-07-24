package roomescape.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.order.domain.Order;
import roomescape.order.exception.InvalidPaymentKeyException;
import roomescape.order.exception.OrderNotFoundException;
import roomescape.order.exception.PaymentAmountMismatchException;
import roomescape.order.repository.OrderRepository;
import roomescape.payment.PaymentConfirmation;
import roomescape.payment.PaymentGateway;
import roomescape.reservation.repository.ReservationRepository;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentConfirmationApplier paymentConfirmationApplier;

    public void confirm(String orderId, String paymentKey, Long amount) {
        if (paymentKey == null) {
            throw new InvalidPaymentKeyException();
        }

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getAmount().equals(amount)) {
            throw new PaymentAmountMismatchException();
        }

        paymentGateway.confirm(
                new PaymentConfirmation(paymentKey, orderId, amount)
        );

        paymentConfirmationApplier.apply(order, paymentKey);
    }

    @Transactional
    public void rollback(String orderId) {
        orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(OrderNotFoundException::new);

        reservationRepository.deleteByOrderId(orderId);
        orderRepository.deleteByOrderId(orderId);
    }
}
