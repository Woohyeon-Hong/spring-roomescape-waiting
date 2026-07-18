package roomescape.payment.exception;

public class UnknownPaymentErrorException extends PaymentApprovalException{

    public UnknownPaymentErrorException() {
        super("결제 승인에 실패했습니다.");
    }
}
