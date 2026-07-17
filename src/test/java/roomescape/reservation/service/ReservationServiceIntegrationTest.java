package roomescape.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import roomescape.order.domain.Order;
import roomescape.order.repository.OrderRepository;
import roomescape.reservation.exception.DuplicateReservationException;
import roomescape.reservation.exception.ReservationNotFoundException;
import roomescape.reservation.service.dto.ReservationCommand;
import roomescape.reservation.service.dto.ReservationUpdateCommand;
import roomescape.support.ConcurrentExecutor;
import roomescape.support.ConcurrentResult;
import roomescape.support.ServiceIntegrationTest;
import roomescape.theme.service.ThemeService;
import roomescape.theme.service.dto.ThemeCommand;
import roomescape.time.service.ReservationTimeService;
import roomescape.time.service.dto.ReservationTimeCommand;

public class ReservationServiceIntegrationTest extends ServiceIntegrationTest {

    private static final long RESERVATION_ID = 1L;

    @Autowired
    ReservationService reservationService;

    @Autowired
    ReservationTimeService reservationTimeService;

    @Autowired
    ThemeService themeService;

    @MockitoSpyBean
    OrderRepository orderRepository;

    @DisplayName("동일한 예약 요청이 동시에 들어오면 하나만 성공하고 나머지는 중복 예외가 발생한다")
    @Test
    void makeReservationTest_duplicate() throws InterruptedException {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url", 1000L
                )
        );

        //when
        List<ConcurrentResult> results = ConcurrentExecutor.executeConcurrently(100, () -> {
            try {
                reservationService.makeReservation(new ReservationCommand(
                        "brown",
                        LocalDate.of(2026, 5, 5),
                        1L,
                        1L
                ));

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
                .allMatch(DuplicateReservationException.class::isInstance);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("예약 생성 중 주문 저장이 실패하면 예약도 저장되지 않는다.")
    @Test
    void makeReservationTest_rolls_back_when_order_save_fails() {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url", 1000L
                )
        );

        doThrow(new DuplicateKeyException("duplicate"))
                .when(orderRepository)
                .save(any(Order.class));

        //when & then
        assertThatThrownBy(() -> reservationService.makeReservation(new ReservationCommand(
                "brown", LocalDate.of(2026, 5, 5), 1L, 1L
        ))).isInstanceOf(DuplicateKeyException.class);

        assertThat(reservationService.findReservations()).isEmpty();
    }

    @DisplayName("서로 다른 본인 예약을 같은 슬롯으로 동시에 수정하면 하나만 성공하고 하나는 중복 예외가 발생한다")
    @Test
    void updateReservationTest_duplicate() throws InterruptedException {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(11, 0))
        );
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(12, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url", 1000L
                )
        );
        reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );
        reservationService.makeReservation(
                new ReservationCommand(
                        "pobi", LocalDate.of(2026, 5, 5), 2L, 1L
                )
        );

        List<ConcurrentResult> results = ConcurrentExecutor.executeConcurrently(List.of(
                () -> updateReservation(1L, "brown"),
                () -> updateReservation(2L, "pobi")
        ));

        //then
        assertThat(results).filteredOn(ConcurrentResult::success).hasSize(1);

        assertThat(results).filteredOn(result -> !result.success()).hasSize(1);
        assertThat(results)
                .filteredOn(result -> !result.success())
                .extracting(ConcurrentResult::exception)
                .allMatch(DuplicateReservationException.class::isInstance);
    }

    private ConcurrentResult updateReservation(Long reservationId, String name) {
        try {
            reservationService.updateReservation(
                    new ReservationUpdateCommand(LocalDate.of(2026, 5, 6), 3L),
                    reservationId,
                    name
            );

            return ConcurrentResult.withSuccess();
        } catch (Throwable e) {
            return ConcurrentResult.withFail(e);
        }
    }

    @DisplayName("예약 삭제 요청이 동시에 들어오면 하나만 성공하고 나머지는 예외가 발생한다.")
    @Test
    void deleteReservationByIdTest_concurrent() throws InterruptedException {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url", 1000L
                )
        );

        reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );

        //when
        List<ConcurrentResult> results = ConcurrentExecutor.executeConcurrently(100, () -> {
            try {
                reservationService.deleteReservationById(RESERVATION_ID);

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
                .allMatch(ReservationNotFoundException.class::isInstance);
    }

    @DisplayName("인가를 포함하는 예약 삭제 요청이 동시에 들어오면 하나만 성공하고 나머지는 예외가 발생한다")
    @Test
    void deleteReservationByIdTest_with_authorization_concurrent() throws InterruptedException {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url", 1000L
                )
        );

        reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );

        //when
        List<ConcurrentResult> results = ConcurrentExecutor.executeConcurrently(100, () -> {
            try {
                reservationService.deleteReservationById(RESERVATION_ID, "brown");

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
                .allMatch(ReservationNotFoundException.class::isInstance);
    }

}
