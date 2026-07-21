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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import roomescape.payment.PaymentConfirmation;
import roomescape.payment.exception.PaymentAlreadyProcessedException;
import roomescape.payment.exception.PaymentConnectionTimeoutException;
import roomescape.payment.exception.PaymentReadTimeoutException;
import roomescape.payment.exception.PaymentTimeoutException;

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

    @DisplayName("결제 승인이 성공하면 아무런 예외를 던지지 않는다.")
    @Test
    void confirm_success() throws InterruptedException, JsonProcessingException {
        //given
        tossPaymentGateway = new TossPaymentGateway(
                TossConfig.buildRestClient(mockWebServer.url("/").toString(), "test_gsk_dummy", 1000, 1000),
                new ObjectMapper()
        );

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
                () -> assertThat(request.getHeader("Content-Type")).contains("application/json"),
                () -> assertThat(request.getHeader( "Idempotency-Key")).isEqualTo("order-1")
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
        //given
        tossPaymentGateway = new TossPaymentGateway(
                TossConfig.buildRestClient(mockWebServer.url("/").toString(), "test_gsk_dummy", 1000, 1000),
                new ObjectMapper()
        );

        enqueue(400, """
        {"code": "ALREADY_PROCESSED_PAYMENT", "message": "이미 처리된 결제 입니다."}
        """);

        //when & then
        assertThatThrownBy(() -> tossPaymentGateway.confirm(
                new PaymentConfirmation("test_pk_1", "order-1", 10000L))
        ).isInstanceOf(PaymentAlreadyProcessedException.class)
                .hasMessage("이미 승인된 결제입니다.");
    }


    @RepeatedTest(10)
    @DisplayName("결제 승인 요청에서 읽기 타임아웃이 발생하면, readTimeoutMS만큼만 기다렸다가 재시도 가능 예외로 실패한다.")
    void confirmTest_read_timeout() {
        //given
        tossPaymentGateway = new TossPaymentGateway(
                TossConfig.buildRestClient(mockWebServer.url("/").toString(), "test_gsk_dummy", 1000, 1300),
                new ObjectMapper()
        );

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
        """, 2);

        //when
        long start = System.nanoTime();

        assertThatThrownBy(() -> tossPaymentGateway.confirm(
                new PaymentConfirmation("test_pk_1", "order-1", 10000L)
        )).isInstanceOf(PaymentReadTimeoutException.class);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        //then
        assertThat(elapsedMs).isLessThan(1800);
    }

    private void enqueue(int statusCode, String body, int delay) {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(statusCode)
                .setHeader("Content-Type", "application/json")
                .setHeadersDelay(delay, TimeUnit.SECONDS)
                .setBody(body));
    }

    @DisplayName("결제 승인 요청 시, 느린 호출이 섞여도 타임아웃이 있으면 성공 TPS가 유지된다.")
    @Test
    void confirmTest_mixed_timeout() {
        //given
        tossPaymentGateway = new TossPaymentGateway(
                TossConfig.buildRestClient(mockWebServer.url("/").toString(), "test_gsk_dummy", 1000, 300),
                new ObjectMapper()
        );

        for (int i = 0; i < 3; i++) {
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
                    """, 2);

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

        }

        //when
        long succeeded = 0;
        long start = System.nanoTime();

        for (int i = 0; i < 6; i++) {
            try {
                tossPaymentGateway.confirm(
                        new PaymentConfirmation("test_pk_1", "order-1", 10000L)
                );
                succeeded++;
            } catch (PaymentTimeoutException e) {
                // 타임아웃으로 일찍 포기한 호출 — 성공 TPS 에 세지 않는다.
            }
        }

        double elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
        double tps = succeeded / elapsedSeconds;

        //then
        assertThat(tps).isGreaterThan(1.1);
    }

    @DisplayName("라우팅이 불가능한 IP로 요청을 보내면, connectTimeoutMs만큼 기다렸다가 SocketTimeout으로 실패한다.")
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
    void confirmTest_non_routable_ip() {
        //given
        String blackholeUrl = "http://10.255.255.1:81";

        TossPaymentGateway gateway = new TossPaymentGateway(
                TossConfig.buildRestClient(blackholeUrl, "test_gsk_dummy", 500, 500),
                new ObjectMapper()
        );

        //when
        long start = System.nanoTime();

        assertThatThrownBy(() -> gateway.confirm(
                new PaymentConfirmation("test_pk_1", "order-1", 10000L))
        ).isInstanceOf(PaymentConnectionTimeoutException.class);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        //then
        assertThat(elapsedMs).isBetween(300L, 2500L);
    }

    @DisplayName("read timeout으로 끊겨 재시도해도 같은 멱등키면 중복 결제가 생기지 않는다.")
    @Test
    void confirmTest_idempotent() {
        //given
        tossPaymentGateway = new TossPaymentGateway(
                TossConfig.buildRestClient(mockWebServer.url("/").toString(), "test_gsk_dummy", 1000, 1000),
                new ObjectMapper()
        );
        PaymentConfirmation confirmation = new PaymentConfirmation("test_pk_1", "order-1", 10000L);

        IdempotentGatewayStub stub = new IdempotentGatewayStub();
        mockWebServer.setDispatcher(stub);

        assertThatThrownBy(() -> tossPaymentGateway.confirm(confirmation))
                .isInstanceOf(PaymentReadTimeoutException.class);

        //when
        assertThatCode(() -> tossPaymentGateway.confirm(confirmation))
                .doesNotThrowAnyException();

        //then
        assertThat(stub.createdPayments()).isEqualTo(1);
        assertThat(stub.seenKeys()).hasSize(2);
        assertThat(stub.seenKeys().get(0)).isNotBlank().isEqualTo(stub.seenKeys().get(1));

        mockWebServer.setDispatcher(new QueueDispatcher());
    }

    private static class IdempotentGatewayStub extends Dispatcher {

        private final Map<String, String> paymentKeyByIdempotencyKey = new ConcurrentHashMap<>();
        private final AtomicInteger createdPayments = new AtomicInteger();
        private final List<String> seenKeys = new CopyOnWriteArrayList<>();

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            var key = request.getHeader("Idempotency-Key");
            seenKeys.add(key);

            if (key == null || key.isBlank()) {
                return slowSuccess(newPaymentKey());
            }

            var existing = paymentKeyByIdempotencyKey.get(key);
            if (existing != null) {
                return fastSuccess(existing);
            }

            var paymentKey = newPaymentKey();
            paymentKeyByIdempotencyKey.put(key, paymentKey);
            return slowSuccess(paymentKey);
        }

        int createdPayments() {
            return createdPayments.get();
        }

        List<String> seenKeys() {
            return seenKeys;
        }

        private String newPaymentKey() {
            return "pk_" + createdPayments.incrementAndGet();
        }

        private MockResponse slowSuccess(String paymentKey) {
            return success(paymentKey).setHeadersDelay(2, TimeUnit.SECONDS);
        }

        private MockResponse fastSuccess(String paymentKey) {
            return success(paymentKey);
        }

        private MockResponse success(String paymentKey) {
            return new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
              {"paymentKey": "%s", "orderId": "order-1", "status": "DONE", "totalAmount": 10000}
              """.formatted(paymentKey));
        }
    }
}
