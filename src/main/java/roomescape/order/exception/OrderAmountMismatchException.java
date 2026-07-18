package roomescape.order.exception;

import roomescape.global.exception.InvalidRequestValueException;

public class OrderAmountMismatchException extends InvalidRequestValueException {

    public OrderAmountMismatchException() {
        super("주문 금액이 예약 금액과 일치하지 않습니다.");
    }
}
