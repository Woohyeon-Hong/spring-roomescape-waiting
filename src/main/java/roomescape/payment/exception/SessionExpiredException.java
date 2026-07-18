package roomescape.payment.exception;

public class SessionExpiredException extends PaymentBadRequestException{

    public SessionExpiredException() {
        super("결제 승인 세션이 만료됐습니다.");
    }
}
