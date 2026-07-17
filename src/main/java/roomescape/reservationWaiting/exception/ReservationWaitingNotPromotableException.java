package roomescape.reservationWaiting.exception;

import roomescape.global.exception.InvalidRequestValueException;

public class ReservationWaitingNotPromotableException extends InvalidRequestValueException {

    public ReservationWaitingNotPromotableException() {
        super("아직 승격할 수 없는 예약 대기입니다.");
    }
}
