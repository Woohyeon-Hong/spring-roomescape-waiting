package roomescape.payment.toss;


public record TossErrorResponse(
        String code,
        String message
) {
}
