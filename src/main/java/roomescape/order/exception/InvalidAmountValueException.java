package roomescape.order.exception;

import roomescape.global.exception.InvalidRequestValueException;

public class InvalidAmountValueException extends InvalidRequestValueException {

    public InvalidAmountValueException() {
        super("주문 금액이 유효하지 않습니다.");
    }
}
