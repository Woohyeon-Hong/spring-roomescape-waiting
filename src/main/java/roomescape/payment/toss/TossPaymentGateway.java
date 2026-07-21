package roomescape.payment.toss;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.SocketTimeoutException;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import roomescape.payment.PaymentConfirmation;
import roomescape.payment.PaymentGateway;
import roomescape.payment.exception.PaymentConnectionTimeoutException;
import roomescape.payment.exception.PaymentReadTimeoutException;

@RequiredArgsConstructor
@Component
public class TossPaymentGateway implements PaymentGateway {

    private static final String CONFIRMATION_URL = "/v1/payments/confirm";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final RestClient tossRestClient;
    private final ObjectMapper objectMapper;

    @Override
    public void confirm(PaymentConfirmation confirmation) {
        try {
            tossRestClient.post()
                    .uri(CONFIRMATION_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(IDEMPOTENCY_KEY, confirmation.orderId())
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
