package roomescape.rateLimit;

import roomescape.global.exception.RetryableException;

public class OutboundRateLimitException extends RetryableException {

    private final long retryAfterSeconds;

    public OutboundRateLimitException(long retryAfterSeconds) {
        super("결제 서버 호출 한도를 초과해 요청을 보내지 않았습니다. 잠시 후 다시 시도해주세요.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
