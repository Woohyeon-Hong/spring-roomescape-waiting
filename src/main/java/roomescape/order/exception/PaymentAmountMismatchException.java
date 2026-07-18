package roomescape.order.exception;

import roomescape.global.exception.InvalidRequestValueException;

public class PaymentAmountMismatchException extends InvalidRequestValueException {

    public PaymentAmountMismatchException() {
        super("결제 금액이 저장된 주문 금액과 일치하지 않습니다.");
    }
}
