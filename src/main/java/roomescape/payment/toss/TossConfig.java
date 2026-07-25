package roomescape.payment.toss;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import roomescape.rateLimit.TokenBucketRateLimiter;

@Configuration
public class TossConfig {

    public static RestClient buildRestClient(String baseUrl, String secret, int connectTimeoutMs, int readTimeoutMs) {
        String basic = Base64.getEncoder()
                .encodeToString((secret + ":").getBytes(StandardCharsets.UTF_8));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .requestFactory(createRequestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
    }

    private static ClientHttpRequestFactory createRequestFactory(int connectTimeoutMs, int readTimeoutMs) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setSocketTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(connectionConfig)
                        .build())
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Bean
    public RestClient tossRestClient(
            @Value("${toss.base-url}") String baseUrl,
            @Value("${toss.secret-key}") String secret,
            @Value("${toss.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${toss.read-timeout-ms}") int readTimeoutMs,
            @Value("${outbound-rate-limit.capacity}") long outboundCapacity,
            @Value("${outbound-rate-limit.refill-per-second}") double outboundRefillPerSec,
            @Value("${gateway.max-attempts}") int maxAttempts
    ) {
        TokenBucketRateLimiter outboundRateLimiter = new TokenBucketRateLimiter(
                outboundCapacity, outboundRefillPerSec, System::nanoTime
        );

        return buildRestClient(baseUrl, secret, connectTimeoutMs, readTimeoutMs)
                .mutate()
                .requestInterceptor(new RetryAfterInterceptor(maxAttempts))
                .requestInterceptor(new OutboundRateLimitInterceptor(outboundRateLimiter))
                .build();
    }
}
