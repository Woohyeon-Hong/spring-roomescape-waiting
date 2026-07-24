package roomescape.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.order.exception.OrderNotFoundException;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.service.ReservationService;
import roomescape.reservation.service.dto.ReservationCommand;
import roomescape.support.ConcurrentExecutor;
import roomescape.support.ConcurrentResult;
import roomescape.support.ServiceIntegrationTest;
import roomescape.theme.service.ThemeService;
import roomescape.theme.service.dto.ThemeCommand;
import roomescape.time.service.ReservationTimeService;
import roomescape.time.service.dto.ReservationTimeCommand;

public class RollbackConcurrencyTest extends ServiceIntegrationTest {

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentConfirmationApplier paymentConfirmationApplier;

    @Autowired
    ReservationTimeService reservationTimeService;

    @Autowired
    ThemeService themeService;

    @Autowired
    ReservationService reservationService;

    @DisplayName("동시에 같은 주문을 롤백 요청하면, 예외가 발생한다.")
    @Test
    void rollbackTest() throws InterruptedException {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url", 1000L
                )
        );

        Reservation reservation = reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );

        paymentConfirmationApplier.apply(reservation.getOrder(), "test-payment-key");

        //when
        List<ConcurrentResult> results = ConcurrentExecutor.executeConcurrently(100, () -> {
            try {
                paymentService.rollback(reservation.getOrder().getOrderId());

                return ConcurrentResult.withSuccess();
            } catch (Throwable e) {
                return ConcurrentResult.withFail(e);
            }
        });

        //then
        assertThat(results).filteredOn(ConcurrentResult::success).hasSize(1);

        assertThat(results).filteredOn(result -> !result.success()).hasSize(99);
        assertThat(results)
                .filteredOn(result -> !result.success())
                .extracting(ConcurrentResult::exception)
                .allMatch(OrderNotFoundException.class::isInstance);
    }
}
