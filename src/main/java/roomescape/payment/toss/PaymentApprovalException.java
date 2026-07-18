package roomescape.payment.toss;

import roomescape.global.exception.BusinessException;

public class PaymentApprovalException extends BusinessException {

    private final String code;

    public PaymentApprovalException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
