package roomescape.payment.exception;

public class InvalidApprovalRequestException extends PaymentBadRequestException{
    public InvalidApprovalRequestException() {
        super("잘못된 승인 요청입니다.");
    }
}
