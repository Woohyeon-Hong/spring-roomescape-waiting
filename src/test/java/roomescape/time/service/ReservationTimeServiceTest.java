package roomescape.time.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import roomescape.reservation.exception.InvalidReservationDateValueException;
import roomescape.theme.exception.ThemeNotFoundException;
import roomescape.theme.repository.ThemeRepository;
import roomescape.time.domain.ReservationTime;
import roomescape.time.exception.DuplicateTimeException;
import roomescape.time.exception.TimeInUseException;
import roomescape.time.exception.TimeNotFoundException;
import roomescape.time.repository.ReservationTimeRepository;
import roomescape.time.service.dto.ReservationTimeCommand;

@ExtendWith(MockitoExtension.class)
class ReservationTimeServiceTest {

    @Mock
    ReservationTimeRepository reservationTimeRepository;

    @Mock
    Clock clock;

    @Mock
    ThemeRepository themeRepository;

    @InjectMocks
    ReservationTimeService reservationTimeService;

    @DisplayName("예약 시간 생성 시, 기존에 이미 동일한 시간이 있으면 예외가 발생한다.")
    @Test
    void registerReservationTimeTest_duplicate() {
        //given
        when(reservationTimeRepository.existByStartAt(LocalTime.of(10, 0)))
                .thenReturn(true);

        //when & then
        assertThatThrownBy(() -> reservationTimeService.registerReservationTime(
                new ReservationTimeCommand(LocalTime.of(10, 0))
        )).isInstanceOf(DuplicateTimeException.class);
    }

    @DisplayName("주어진 날짜와 테마의 예약 가능 시간 조회 시, 날짜가 오늘까지면 예외가 발생한다.")
    @Test
    void findAvailableReservationTimesTest_invalid_date() {
        //given
        when(clock.instant()).thenReturn(
                LocalDate.of(2026, 5, 8)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        LocalDate today = LocalDate.of(2026, 5, 8);
        LocalDate previous = LocalDate.of(2026, 5, 7);

        //when & then
        assertThatThrownBy(() -> reservationTimeService.findAvailableReservationTimes(1L, today))
                .isInstanceOf(InvalidReservationDateValueException.class);

        assertThatThrownBy(() -> reservationTimeService.findAvailableReservationTimes(1L, previous))
                .isInstanceOf(InvalidReservationDateValueException.class);
    }

    @DisplayName("주어진 날짜와 테마의 예약 가능 시간 조회 시, 테마가 존재하지 않으면 예외가 발생한다.")
    @Test
    void findAvailableReservationTimesTest_not_found_theme() {
        //given
        when(clock.instant()).thenReturn(
                LocalDate.of(2026, 5, 8)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        when(themeRepository.findById(1L))
                .thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> reservationTimeService.findAvailableReservationTimes(1L,  LocalDate.of(2026, 5, 9)))
                .isInstanceOf(ThemeNotFoundException.class);
    }

    @DisplayName("id에 해당하는 테마가 없으면 예외가 발생한다.")
    @Test
    void removeThemeByIdTest_not_found() {
        //given
        when(reservationTimeRepository.findById(1L))
                .thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> reservationTimeService.removeReservationTimeById(1L))
                .isInstanceOf(TimeNotFoundException.class);
    }

    @DisplayName("예약 시간 삭제시, 예약 시간이 사용 중이면 예외가 발생한다.")
    @Test
    void removeThemeByIdTest_in_use() {
        //given
        when(reservationTimeRepository.findById(1L))
                .thenReturn(Optional.of(new ReservationTime(1L, LocalTime.of(10, 0))));

        when(reservationTimeRepository.deleteById(1L))
                .thenThrow(new DataIntegrityViolationException("foreign key"));

        //when & then
        assertThatThrownBy(() -> reservationTimeService.removeReservationTimeById(1L))
                .isInstanceOf(TimeInUseException.class);
    }
}
