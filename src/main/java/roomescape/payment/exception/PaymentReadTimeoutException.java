package roomescape.payment.exception;

public class PaymentReadTimeoutException extends PaymentTimeoutException {

    public PaymentReadTimeoutException() {
        super("결제 서버 응답이 없어 승인 여부를 확인할 수 없습니다. 잠시 후 다시 확인해주세요.");
    }
}
