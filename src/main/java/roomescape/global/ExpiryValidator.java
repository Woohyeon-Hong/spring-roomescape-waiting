package roomescape.global;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import roomescape.reservation.exception.InvalidReservationDateValueException;

@RequiredArgsConstructor
@Component
public class ExpiryValidator {

    private final Clock clock;

    public void validateFromNextDay(LocalDate date) {
        LocalDate nowDate = LocalDate.now(clock);

        if (!date.isAfter(nowDate)) {
            throw new InvalidReservationDateValueException();
        }
    }

}
