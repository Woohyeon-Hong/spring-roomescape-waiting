package roomescape.payment.exception;

public class PaymentRejectedException extends PaymentApprovalException{

    public PaymentRejectedException() {
        super("카드가 거절됐습니다.");
    }
}
