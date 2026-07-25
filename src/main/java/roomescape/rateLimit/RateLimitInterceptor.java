package roomescape.rateLimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;


@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final TokenBucketRateLimiter tokenBucketRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {

        if (!tokenBucketRateLimiter.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER,
                    String.valueOf(tokenBucketRateLimiter.retryAfterSeconds()));

            return false;
        }

        return true;
    }
}
