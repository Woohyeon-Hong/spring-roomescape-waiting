package roomescape.payment.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.payment.PaymentConfirmation;
import roomescape.payment.exception.PaymentAlreadyProcessedException;

class TossPaymentGatewayTest {

    static MockWebServer mockWebServer;
    TossPaymentGateway tossPaymentGateway;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        tossPaymentGateway = new TossPaymentGateway(
                new ObjectMapper(),
                mockWebServer.url("/").toString(),
                "test_gsk_dummy"
        );
    }

    @DisplayName("결제 승인이 성공하면 아무런 예외를 던지지 않는다.")
    @Test
    void confirm_success() throws InterruptedException, JsonProcessingException {
        //given
        enqueue(200, """
        {
          "paymentKey": "test_pk_1",
          "orderId": "order-1",
          "orderName": "방탈출 예약",
          "status": "DONE",
          "totalAmount": 10000,
          "balanceAmount": 10000,
          "method": "카드",
          "approvedAt": "2026-06-08T12:00:00+09:00",
          "requestedAt": "2026-06-08T11:59:30+09:00"
        }
        """);

        //when
        assertThatCode(() -> tossPaymentGateway.confirm(
                new PaymentConfirmation("test_pk_1", "order-1", 10000L))
        ).doesNotThrowAnyException();

        //then
        RecordedRequest request = mockWebServer.takeRequest();
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString("test_gsk_dummy:".getBytes(StandardCharsets.UTF_8));

        assertAll(
                () -> assertThat(request.getMethod()).isEqualTo("POST"),
                () -> assertThat(request.getPath()).isEqualTo("/v1/payments/confirm"),
                () -> assertThat(request.getHeader("Authorization")).isEqualTo(basicAuth),
                () -> assertThat(request.getHeader("Content-Type")).contains("application/json")
        );

        JsonNode body = new ObjectMapper().readTree(request.getBody().readUtf8());
        assertAll(
                () -> assertThat(body.get("paymentKey").asText()).isEqualTo("test_pk_1"),
                () -> assertThat(body.get("orderId").asText()).isEqualTo("order-1"),
                () -> assertThat(body.get("amount").asLong()).isEqualTo(10000L)
        );
    }

    private void enqueue(int statusCode, String body) {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(statusCode)
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }

    @DisplayName("이미 승인된 결제면 PaymentAlreadyProcessedException이 발생한다.")
    @Test
    void confirm_fail() {
        enqueue(400, """
        {"code": "ALREADY_PROCESSED_PAYMENT", "message": "이미 처리된 결제 입니다."}
        """);

        assertThatThrownBy(() -> tossPaymentGateway.confirm(
                new PaymentConfirmation("test_pk_1", "order-1", 10000L))
        ).isInstanceOf(PaymentAlreadyProcessedException.class)
                .hasMessage("이미 승인된 결제입니다.");
    }
}
