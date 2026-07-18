package roomescape.payment.exception;

public class PaymentAlreadyProcessedException extends PaymentApprovalException{

    public PaymentAlreadyProcessedException() {
        super("이미 승인된 결제입니다.");
    }
}
