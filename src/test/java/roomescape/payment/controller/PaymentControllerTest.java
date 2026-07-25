package roomescape.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import roomescape.payment.exception.PaymentReadTimeoutException;
import roomescape.payment.service.PaymentService;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PaymentService paymentService;

    @DisplayName("orderId, paymentKey, amount를 받아 결제를 승인하고 204를 반환한다.")
    @Test
    void confirmTest_success() throws Exception {
        String body = """
                {
                    "paymentKey": "payment-key",
                    "amount": 1000
                }
                """;

        mockMvc.perform(
                post("/payments/{orderId}/confirm", "order-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isNoContent());

        verify(paymentService).confirm("order-id", "payment-key", 1000L);
    }

    @DisplayName("orderId를 받아, 주문과 예약을 삭제하고 204를 반환한다.")
    @Test
    void failTest_success() throws Exception {
        mockMvc.perform(
                delete("/payments/{orderId}/fail", "order-id")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent());
    }

    @DisplayName("승인 응답 지연(read timeout) 시 승인 여부가 불확실하므로 504를 반환한다.")
    @Test
    void confirmTest_readTimeout_returns504() throws Exception {
        String body = """
                {
                    "paymentKey": "payment-key",
                    "amount": 1000
                }
                """;

        doThrow(new PaymentReadTimeoutException())
                .when(paymentService).confirm(anyString(), any(), anyLong());

        mockMvc.perform(
                post("/payments/{orderId}/confirm", "order-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isGatewayTimeout());
    }
}
