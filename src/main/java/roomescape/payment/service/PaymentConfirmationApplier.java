package roomescape.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.order.domain.Order;
import roomescape.order.repository.OrderRepository;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.exception.ReservationNotFoundException;
import roomescape.reservation.repository.ReservationRepository;

@RequiredArgsConstructor
@Service
public class PaymentConfirmationApplier {

    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public void apply(Order order, String paymentKey) {
        Order updatedOrder = order.updatePaymentKey(paymentKey);
        orderRepository.update(updatedOrder);

        Reservation confirmed = reservationRepository.findByOrderId(order.getOrderId())
                .orElseThrow(ReservationNotFoundException::new)
                .confirm();

        reservationRepository.update(confirmed);
    }
}
