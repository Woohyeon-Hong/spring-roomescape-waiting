package roomescape.order.exception;

import roomescape.global.exception.InvalidRequestFormatException;

public class InvalidPaymentKeyException extends InvalidRequestFormatException {

    public InvalidPaymentKeyException() {
        super("paymentKey가 null이면 안됩니다.");
    }
}
