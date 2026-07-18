package roomescape.order.exception;

import roomescape.global.exception.InvalidRequestValueException;

public class OrderNotConfirmedException extends InvalidRequestValueException {

    public OrderNotConfirmedException() {
        super("결제가 확정되지 않은 주문입니다.");
    }
}