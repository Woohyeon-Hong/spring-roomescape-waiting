package roomescape.global;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import roomescape.reservation.exception.InvalidReservationDateValueException;

@ExtendWith(MockitoExtension.class)
class ExpiryValidatorTest {

    @Mock
    Clock clock;

    @InjectMocks
    ExpiryValidator expiryValidator;

    @DisplayName("주어진 날짜가 오늘까지면 예외가 발생한다.")
    @Test
    void validateFromNextDayTest() {
        //given
        LocalDate today = LocalDate.of(2026, 5, 15);

        when(clock.instant()).thenReturn(
                LocalDate.of(2026, 5, 15)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        //when & then
        assertThatThrownBy(() ->
                expiryValidator.validateFromNextDay(today))
                .isInstanceOf(InvalidReservationDateValueException.class);
    }
}
