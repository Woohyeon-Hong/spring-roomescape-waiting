package roomescape.payment.toss;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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
            @Value("${toss.secret-key}") String secret
    ) {
        String basic = Base64.getEncoder()
                .encodeToString((secret + ":").getBytes(StandardCharsets.UTF_8));

        this.tossRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
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
                    TossErrorResponse error = objectMapper.readValue(res.getBody(), TossErrorResponse.class);
                    throw new PaymentApprovalException(error.message(), error.code());
                }).toBodilessEntity();
    }
}
