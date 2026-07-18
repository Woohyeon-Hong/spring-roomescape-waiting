package roomescape.payment.exception;

public abstract class PaymentBadRequestException extends PaymentApprovalException{
    protected PaymentBadRequestException(String message) {
        super(message);
    }
}
