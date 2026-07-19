package roomescape.payment.exception;

public class PaymentConnectionTimeoutException extends PaymentTimeoutException {

    public PaymentConnectionTimeoutException() {
        super("결제 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}
