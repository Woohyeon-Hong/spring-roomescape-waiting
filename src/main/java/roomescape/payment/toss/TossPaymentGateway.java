package roomescape.payment.toss;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import roomescape.payment.PaymentConfirmation;
import roomescape.payment.PaymentGateway;

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

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        this.tossRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .requestFactory(factory)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void confirm(PaymentConfirmation confirmation) {
        tossRestClient.post()
                .uri("/v1/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(confirmation)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw objectMapper.readValue(res.getBody(), TossErrorResponse.class)
                            .toPaymentApprovalException();
                }).toBodilessEntity();
    }
}
