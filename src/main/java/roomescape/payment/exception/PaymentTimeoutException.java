package roomescape.payment.exception;

public abstract class PaymentTimeoutException extends PaymentApprovalException {

    protected PaymentTimeoutException(String message) {
        super(message);
    }
}
