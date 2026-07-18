package roomescape.payment.exception;

public class PaymentServerErrorException extends PaymentApprovalException{

    public PaymentServerErrorException() {
        super("PG 사 내부 오류가 발생했습니다.");
    }
}
