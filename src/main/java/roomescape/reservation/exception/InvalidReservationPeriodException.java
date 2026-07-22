package roomescape.reservation.exception;

import roomescape.global.exception.InvalidRequestValueException;

public class InvalidReservationPeriodException extends InvalidRequestValueException {

    public InvalidReservationPeriodException() {
        super("유효하지 않은 예약 날짜 범위입니다.");
    }
}
