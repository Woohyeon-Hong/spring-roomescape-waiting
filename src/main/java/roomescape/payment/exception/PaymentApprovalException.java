package roomescape.payment.exception;

import roomescape.global.exception.BusinessException;

public abstract class PaymentApprovalException extends BusinessException {

    protected PaymentApprovalException(String message) {
        super(message);
    }
}
