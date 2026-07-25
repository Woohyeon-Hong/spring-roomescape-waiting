package roomescape.payment.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import roomescape.rateLimit.OutboundRateLimitException;
import roomescape.rateLimit.TokenBucketRateLimiter;

class OutboundRateLimitInterceptorTest {

    @DisplayName("토큰을 소비할 수 있으면 요청을 그대로 실행하고 응답을 반환한다.")
    @Test
    void interceptTest_passesThrough_whenTokenAvailable() throws Exception {
        //given
        AtomicLong clock = new AtomicLong(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(1, 1.0, clock::get);
        OutboundRateLimitInterceptor interceptor = new OutboundRateLimitInterceptor(limiter);

        HttpRequest request = mock(HttpRequest.class);
        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(request, body)).thenReturn(response);

        //when
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        //then
        assertThat(result).isSameAs(response);
        verify(execution).execute(request, body);
    }

    @DisplayName("토큰이 없으면 요청을 보내지 않고, retryAfterSeconds를 담은 OutboundRateLimitException을 던진다.")
    @Test
    void interceptTest_blocksRequest_whenNoTokenAvailable() throws Exception {
        //given
        AtomicLong clock = new AtomicLong(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(1, 1.0, clock::get);
        OutboundRateLimitInterceptor interceptor = new OutboundRateLimitInterceptor(limiter);
        limiter.tryConsume(); // 토큰 소진

        HttpRequest request = mock(HttpRequest.class);
        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        //when & then
        assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
                .isInstanceOf(OutboundRateLimitException.class)
                .extracting(e -> ((OutboundRateLimitException) e).retryAfterSeconds())
                .isEqualTo(1L);

        verify(execution, never()).execute(any(), any());
    }
}
