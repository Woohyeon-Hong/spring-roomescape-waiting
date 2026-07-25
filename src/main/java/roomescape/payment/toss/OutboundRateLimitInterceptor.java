package roomescape.payment.toss;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import roomescape.rateLimit.OutboundRateLimitException;
import roomescape.rateLimit.TokenBucketRateLimiter;

@RequiredArgsConstructor
public class OutboundRateLimitInterceptor implements ClientHttpRequestInterceptor {

    private final TokenBucketRateLimiter tokenBucketRateLimiter;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (!tokenBucketRateLimiter.tryConsume()) {
            throw new OutboundRateLimitException(tokenBucketRateLimiter.retryAfterSeconds());
        }

        return execution.execute(request, body);
    }
}
