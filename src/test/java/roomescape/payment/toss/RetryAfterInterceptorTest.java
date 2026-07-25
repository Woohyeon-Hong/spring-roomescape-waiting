package roomescape.payment.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class RetryAfterInterceptorTest {

    private final HttpRequest request = mock(HttpRequest.class);
    private final byte[] body = new byte[0];
    private final List<Long> sleptSeconds = new ArrayList<>();
    private final LongConsumer fakeSleeper = sleptSeconds::add;

    @DisplayName("429가 아니면 재시도 없이 응답을 그대로 반환한다.")
    @Test
    void interceptTest_returnsImmediately_whenNotTooManyRequests() throws IOException {
        //given
        RetryAfterInterceptor interceptor = new RetryAfterInterceptor(3, fakeSleeper);
        ClientHttpResponse okResponse = mockResponse(HttpStatus.OK, null);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(request, body)).thenReturn(okResponse);

        //when
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        //then
        assertThat(result).isSameAs(okResponse);
        verify(execution, times(1)).execute(request, body);
        assertThat(sleptSeconds).isEmpty();
    }

    @DisplayName("429 응답에 Retry-After가 있으면 그 값만큼 대기 후 재시도해서, 이후 성공하면 그 응답을 반환한다.")
    @Test
    void interceptTest_retriesAfterRetryAfterHeader_thenSucceeds() throws IOException {
        //given
        RetryAfterInterceptor interceptor = new RetryAfterInterceptor(3, fakeSleeper);
        ClientHttpResponse tooManyRequests = mockResponse(HttpStatus.TOO_MANY_REQUESTS, "5");
        ClientHttpResponse okResponse = mockResponse(HttpStatus.OK, null);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(request, body)).thenReturn(tooManyRequests, okResponse);

        //when
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        //then
        assertThat(result).isSameAs(okResponse);
        verify(execution, times(2)).execute(request, body);
        assertThat(sleptSeconds).containsExactly(5L);
    }

    @DisplayName("Retry-After 헤더가 없으면 기본 간격(1초)으로 재시도한다.")
    @Test
    void interceptTest_fallsBackToDefaultInterval_whenNoRetryAfterHeader() throws IOException {
        //given
        RetryAfterInterceptor interceptor = new RetryAfterInterceptor(3, fakeSleeper);
        ClientHttpResponse tooManyRequests = mockResponse(HttpStatus.TOO_MANY_REQUESTS, null);
        ClientHttpResponse okResponse = mockResponse(HttpStatus.OK, null);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(request, body)).thenReturn(tooManyRequests, okResponse);

        //when
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        //then
        assertThat(result).isSameAs(okResponse);
        verify(execution, times(2)).execute(request, body);
        assertThat(sleptSeconds).containsExactly(1L);
    }

    @DisplayName("maxAttempts에 도달할 때까지 계속 429면 더 재시도하지 않고 마지막 429 응답을 그대로 반환한다.")
    @Test
    void interceptTest_stopsAtMaxAttempts_whenAlwaysTooManyRequests() throws IOException {
        //given
        int maxAttempts = 2;
        RetryAfterInterceptor interceptor = new RetryAfterInterceptor(maxAttempts, fakeSleeper);
        ClientHttpResponse tooManyRequests = mockResponse(HttpStatus.TOO_MANY_REQUESTS, "0");
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(request, body)).thenReturn(tooManyRequests);

        //when
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        //then
        assertThat(result).isSameAs(tooManyRequests);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(execution, times(maxAttempts)).execute(any(), any());
        assertThat(sleptSeconds).containsExactly(0L);
    }

    private ClientHttpResponse mockResponse(HttpStatus status, String retryAfterHeader) throws IOException {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(status);

        HttpHeaders headers = new HttpHeaders();
        if (retryAfterHeader != null) {
            headers.set(HttpHeaders.RETRY_AFTER, retryAfterHeader);
        }
        when(response.getHeaders()).thenReturn(headers);

        return response;
    }
}
