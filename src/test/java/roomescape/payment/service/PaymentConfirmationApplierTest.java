package roomescape.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import roomescape.order.domain.Order;
import roomescape.order.exception.OrderNotFoundException;
import roomescape.order.repository.OrderRepository;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.reservation.exception.ReservationNotFoundException;
import roomescape.reservation.repository.ReservationRepository;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmationApplierTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    ReservationRepository reservationRepository;

    @InjectMocks
    PaymentConfirmationApplier paymentConfirmationApplier;

    @DisplayName("결제 결과를 적용하면, payment 키를 주문에 반영하고 연결된 예약을 확정한다.")
    @Test
    void applyTest_success() {
        //given
        Order order = new Order(1L, "order-id", 1000L, null);

        when(reservationRepository.findByOrderId("order-id"))
                .thenReturn(Optional.of(new Reservation(
                        1L,
                        "brown",
                        LocalDate.of(2026, 5, 15),
                        ReservationStatus.PENDING,
                        new ReservationTime(1L, LocalTime.of(10, 0)),
                        new Theme(1L, "이름", "설명", "thumbnailUrl", 1000L),
                        order
                )));

        //when
        paymentConfirmationApplier.apply(order, "payment-key");

        //then
        verify(orderRepository).update(order.updatePaymentKey("payment-key"));
        verify(reservationRepository).update(any(Reservation.class));
    }

    @DisplayName("주문이 없으면 예외가 발생한다.")
    @Test
    void applyTest_order_not_found() {
        //given
        Order order = new Order(1L, "order-id", 1000L, null);

        doThrow(OrderNotFoundException.class)
                .when(orderRepository)
                        .update(any(Order.class));

        //when & then
        assertThatThrownBy(() -> paymentConfirmationApplier.apply(order, "payment-key"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @DisplayName("주문에 연결된 예약이 없으면 예외가 발생한다.")
    @Test
    void applyTest_reservation_not_found() {
        //given
        Order order = new Order(1L, "order-id", 1000L, null);

        when(reservationRepository.findByOrderId("order-id"))
                .thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> paymentConfirmationApplier.apply(order, "payment-key"))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
