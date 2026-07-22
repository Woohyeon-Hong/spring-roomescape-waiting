package roomescape.payment.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import roomescape.payment.service.PaymentService;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PaymentService paymentService;

    @DisplayName("orderId, paymentKey, amount를 받아 결제를 승인하고 204를 반환한다.")
    @Test
    void confirm_success() throws Exception {
        String body = """
                {
                    "paymentKey": "payment-key",
                    "amount": 1000
                }
                """;

        mockMvc.perform(
                post("/orders/{orderId}/confirm", "order-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isNoContent());

        verify(paymentService).confirm("order-id", "payment-key", 1000L);
    }
}
