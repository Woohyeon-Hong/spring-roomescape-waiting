package roomescape.rateLimit;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import roomescape.payment.controller.PaymentController;
import roomescape.payment.service.PaymentService;

@WebMvcTest(PaymentController.class)
@TestPropertySource(properties = {
        "rate-limit.capacity=1",
        "rate-limit.refill-per-second=0.001"
})
class RateLimitInterceptorTest {

    private static final String CONFIRM_BODY = """
            {
                "paymentKey": "payment-key",
                "amount": 1000
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PaymentService paymentService;

    @DisplayName("capacity를 넘지 않는 요청은 정상 처리되고, 넘는 요청은 429와 Retry-After 헤더를 받는다.")
    @Test
    void rateLimitTest() throws Exception {
        mockMvc.perform(post("/payments/order-id/confirm").contentType(APPLICATION_JSON).content(CONFIRM_BODY))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/payments/order-id/confirm").contentType(APPLICATION_JSON).content(CONFIRM_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
