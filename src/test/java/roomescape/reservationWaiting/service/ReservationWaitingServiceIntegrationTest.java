package roomescape.reservationWaiting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.exception.DuplicateReservationException;
import roomescape.reservation.repository.ReservationRepository;
import roomescape.reservation.service.ReservationService;
import roomescape.reservation.service.dto.ReservationCommand;
import roomescape.reservation.service.dto.ReservationUpdateCommand;
import roomescape.reservationWaiting.domain.ReservationWaiting;
import roomescape.reservationWaiting.exception.DuplicateReservationWaitingException;
import roomescape.reservationWaiting.exception.ReservationWaitingNotFoundException;
import roomescape.reservationWaiting.repository.ReservationWaitingRepository;
import roomescape.reservationWaiting.service.dto.ReservationWaitingCommand;
import roomescape.support.ConcurrentExecutor;
import roomescape.support.ConcurrentResult;
import roomescape.support.ServiceIntegrationTest;
import roomescape.theme.service.ThemeService;
import roomescape.theme.service.dto.ThemeCommand;
import roomescape.time.service.ReservationTimeService;
import roomescape.time.service.dto.ReservationTimeCommand;

public class ReservationWaitingServiceIntegrationTest extends ServiceIntegrationTest {

    private static final long RESERVATION_ID = 1L;
    private static final long WAITING_ID = 1L;

    @Autowired
    ReservationTimeService reservationTimeService;

    @Autowired
    ThemeService themeService;

    @Autowired
    ReservationWaitingService reservationWaitingService;

    @Autowired
    ReservationService reservationService;

    @MockitoSpyBean
    ReservationWaitingRepository reservationWaitingRepository;

    @MockitoSpyBean
    ReservationRepository reservationRepository;

    @DisplayName("동일한 예약 대기 신청이 동시에 들어오면 하나만 성공하고 나머지는 중복 예외가 발생한다")
    @Test
    void makeReservationWaitingTest_duplicate() throws InterruptedException {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url"
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
                reservationWaitingService.makeReservationWaiting(new ReservationWaitingCommand(
                        "pobi",
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
                .allMatch(DuplicateReservationWaitingException.class::isInstance);
    }

    @DisplayName("예약 대기 생성 중에는 동일 슬롯의 예약을 삭제/변경할 수 없다.")
    @Test
    void makeReservationWaitingTest_update_lock() throws Exception {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(11, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url"
                )
        );

        reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CountDownLatch waitingSaveEntered = new CountDownLatch(1);
        CountDownLatch allowWaitingSave = new CountDownLatch(1);

        doAnswer(invocation -> {
            waitingSaveEntered.countDown();
            assertThat(allowWaitingSave.await(2, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(reservationWaitingRepository).save(any(ReservationWaiting.class));

        try {
            Future<ReservationWaiting> waitingFuture = executorService.submit(() ->
                    reservationWaitingService.makeReservationWaiting(
                            new ReservationWaitingCommand(
                                    "pobi",
                                    LocalDate.of(2026, 5, 5),
                                    1L,
                                    1L
                            )
                    )
            );

            assertThat(waitingSaveEntered.await(2, TimeUnit.SECONDS)).isTrue();

            Future<?> updateFuture = executorService.submit(() ->
                    reservationService.updateReservation(
                            new ReservationUpdateCommand(LocalDate.of(2026, 5, 5), 2L),
                            RESERVATION_ID,
                            "brown"
                    )
            );

            Thread.sleep(200);
            assertThat(updateFuture.isDone()).isFalse();

            //when
            allowWaitingSave.countDown();
            waitingFuture.get(2, TimeUnit.SECONDS);
            updateFuture.get(2, TimeUnit.SECONDS);

            //then
            assertAll(
                    () -> assertReservationName(LocalDate.of(2026, 5, 5), 2L, 1L, "brown"),
                    () -> assertWaitingExists(WAITING_ID)
            );
        } finally {
            executorService.shutdownNow();
        }
    }

    @DisplayName("예약 대기 생성 중에는 동일 슬롯의 예약을 삭제할 수 없다.")
    @Test
    void makeReservationWaitingTest_delete_lock() throws Exception {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url"
                )
        );

        reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CountDownLatch waitingSaveEntered = new CountDownLatch(1);
        CountDownLatch allowWaitingSave = new CountDownLatch(1);

        doAnswer(invocation -> {
            waitingSaveEntered.countDown();
            assertThat(allowWaitingSave.await(2, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(reservationWaitingRepository).save(any(ReservationWaiting.class));

        try {
            Future<ReservationWaiting> waitingFuture = executorService.submit(() ->
                    reservationWaitingService.makeReservationWaiting(
                            new ReservationWaitingCommand(
                                    "pobi",
                                    LocalDate.of(2026, 5, 5),
                                    1L,
                                    1L
                            )
                    )
            );

            assertThat(waitingSaveEntered.await(2, TimeUnit.SECONDS)).isTrue();

            Future<?> deleteFuture = executorService.submit(() ->
                    reservationService.deleteReservationById(RESERVATION_ID)
            );

            Thread.sleep(200);
            assertThat(deleteFuture.isDone()).isFalse();

            //when
            allowWaitingSave.countDown();
            waitingFuture.get(2, TimeUnit.SECONDS);
            deleteFuture.get(2, TimeUnit.SECONDS);

            //then
            assertWaitingExists(WAITING_ID);
        } finally {
            executorService.shutdownNow();
        }
    }

    @DisplayName("예약 대기 삭제 요청이 동시에 들어오면 하나만 성공하고 나머지는 예외가 발생한다")
    @Test
    void deleteReservationWaitingTest_duplicate() throws InterruptedException {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url"
                )
        );

        reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );
        reservationWaitingService.makeReservationWaiting(
                new ReservationWaitingCommand(
                        "pobi", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );

        //when
        List<ConcurrentResult> results = ConcurrentExecutor.executeConcurrently(100, () -> {
            try {
                reservationWaitingService.deleteReservationWaitingById(WAITING_ID, "pobi");

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
                .allMatch(ReservationWaitingNotFoundException.class::isInstance);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("예약 대기 승격 시 예약 저장이 실패하면 대기 삭제가 롤백된다.")
    @Test
    void promoteWaitingTest_rolls_back_when_save_fails() {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url"
                )
        );

        reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );
        reservationWaitingService.makeReservationWaiting(new ReservationWaitingCommand(
                "pobi",
                LocalDate.of(2026, 5, 5),
                1L,
                1L
        ));
        reservationService.deleteReservationById(RESERVATION_ID);

        doThrow(new DuplicateKeyException("duplicate"))
                .when(reservationRepository)
                .save(any(Reservation.class));

        //when & then
        assertThatThrownBy(() -> reservationWaitingService.promoteWaiting(WAITING_ID, "pobi"))
                .isInstanceOf(DuplicateReservationException.class);

        assertWaitingExists(WAITING_ID);
    }

    @DisplayName("동일한 예약 대기를 동시에 승격 요청하면 하나만 성공하고 나머지는 예외가 발생한다.")
    @Test
    void promoteWaitingTest_concurrent() throws InterruptedException {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url"
                )
        );

        reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );
        reservationWaitingService.makeReservationWaiting(new ReservationWaitingCommand(
                "pobi",
                LocalDate.of(2026, 5, 5),
                1L,
                1L
        ));
        reservationService.deleteReservationById(RESERVATION_ID);

        //when
        List<ConcurrentResult> results = ConcurrentExecutor.executeConcurrently(10, () -> {
            try {
                reservationWaitingService.promoteWaiting(WAITING_ID, "pobi");

                return ConcurrentResult.withSuccess();
            } catch (Throwable e) {
                return ConcurrentResult.withFail(e);
            }
        });

        //then
        assertThat(results).filteredOn(ConcurrentResult::success).hasSize(1);

        assertThat(results).filteredOn(result -> !result.success()).hasSize(9);
        assertThat(results)
                .filteredOn(result -> !result.success())
                .extracting(ConcurrentResult::exception)
                .allMatch(ReservationWaitingNotFoundException.class::isInstance);
    }

    @DisplayName("승격 가능한 예약 대기 목록을 조회하면, 대기열 1순위이면서 슬롯이 비어있는 대기만 반환된다.")
    @Test
    void findPromotableWaitingsTest() {
        //given
        reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        );
        themeService.registerTheme(
                new ThemeCommand(
                        "테마", "설명", "url"
                )
        );

        reservationService.makeReservation(
                new ReservationCommand(
                        "brown", LocalDate.of(2026, 5, 5), 1L, 1L
                )
        );
        reservationWaitingService.makeReservationWaiting(new ReservationWaitingCommand(
                "pobi",
                LocalDate.of(2026, 5, 5),
                1L,
                1L
        ));

        //when & then
        assertThat(reservationWaitingService.findPromotableWaitings("pobi")).isEmpty();

        //when
        reservationService.deleteReservationById(RESERVATION_ID);

        //then
        assertThat(reservationWaitingService.findPromotableWaitings("pobi"))
                .extracting(ReservationWaiting::getId)
                .containsExactly(WAITING_ID);
    }
}
