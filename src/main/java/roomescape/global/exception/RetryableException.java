package roomescape.global.exception;

public abstract class RetryableException extends BusinessException {

    protected RetryableException(String message) {
        super(message);
    }

    public abstract long retryAfterSeconds();
}
