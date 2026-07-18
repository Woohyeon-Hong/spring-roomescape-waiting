package roomescape.payment.exception;

public class PaymentUnauthorizedException extends PaymentApprovalException{

    public PaymentUnauthorizedException() {
        super("인가되지 않은 결제 승인 요청입니다.");
    }
}
