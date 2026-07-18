package roomescape.payment.toss;


import roomescape.payment.exception.DuplicateOrderIdException;
import roomescape.payment.exception.InvalidApprovalRequestException;
import roomescape.payment.exception.PaymentAlreadyProcessedException;
import roomescape.payment.exception.PaymentApprovalException;
import roomescape.payment.exception.PaymentNotFoundException;
import roomescape.payment.exception.PaymentRejectedException;
import roomescape.payment.exception.PaymentServerErrorException;
import roomescape.payment.exception.PaymentUnauthorizedException;
import roomescape.payment.exception.SessionExpiredException;
import roomescape.payment.exception.UnknownPaymentErrorException;

record TossErrorResponse(
        String code,
        String message
) {

    PaymentApprovalException toPaymentApprovalException() {
        return switch (code) {
            case "ALREADY_PROCESSED_PAYMENT" -> new PaymentAlreadyProcessedException();
            case "DUPLICATED_ORDER_ID" -> new DuplicateOrderIdException();
            case "NOT_FOUND_PAYMENT_SESSION" -> new SessionExpiredException();
            case "INVALID_REQUEST" -> new InvalidApprovalRequestException();
            case "UNAUTHORIZED_KEY", "INVALID_API_KEY" -> new PaymentUnauthorizedException();
            case "REJECT_CARD_PAYMENT" -> new PaymentRejectedException();
            case "NOT_FOUND_PAYMENT" -> new PaymentNotFoundException();
            case "FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING" -> new PaymentServerErrorException();
            default -> new UnknownPaymentErrorException();
        };
    }
}
