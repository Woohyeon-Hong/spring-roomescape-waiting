package roomescape.payment.exception;

public class DuplicateOrderIdException extends PaymentBadRequestException{

    public DuplicateOrderIdException() {
        super("결제 승인 요청이 중복됐습니다.");
    }
}
