package roomescape.payment.toss;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import roomescape.payment.PaymentConfirmation;
import roomescape.payment.PaymentGateway;
import roomescape.payment.exception.PaymentConnectionTimeoutException;
import roomescape.payment.exception.PaymentReadTimeoutException;

@Component
public class TossPaymentGateway implements PaymentGateway {

    private final RestClient tossRestClient;
    private final ObjectMapper objectMapper;

    public TossPaymentGateway(
            ObjectMapper objectMapper,
            @Value("${toss.base-url}") String baseUrl,
            @Value("${toss.secret-key}") String secret,
            @Value("${toss.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${toss.read-timeout-ms}") int readTimeoutMs
    ) {
        String basic = Base64.getEncoder()
                .encodeToString((secret + ":").getBytes(StandardCharsets.UTF_8));

        this.tossRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .requestFactory(createRequestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
        this.objectMapper = objectMapper;
    }

    private ClientHttpRequestFactory createRequestFactory(int connectTimeoutMs, int readTimeoutMs) {
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

    @Override
    public void confirm(PaymentConfirmation confirmation) {
        try {
            tossRestClient.post()
                    .uri("/v1/payments/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(confirmation)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleError)
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ConnectTimeoutException) {
                throw new PaymentConnectionTimeoutException();
            }
            if (cause instanceof SocketTimeoutException) {
                throw new PaymentReadTimeoutException();
            }

            throw e;
        }
    }

    private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
        throw objectMapper.readValue(response.getBody(), TossErrorResponse.class)
                .toPaymentApprovalException();
    }
}
