package roomescape.payment.exception;

public class PaymentNotFoundException extends PaymentApprovalException{

    public PaymentNotFoundException() {
        super("결제가 존재하지 않습니다.");
    }
}
