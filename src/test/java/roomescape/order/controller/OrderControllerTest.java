package roomescape.order.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import roomescape.order.service.OrderService;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @DisplayName("orderId에 해당하는 주문을 삭제하고 204를 반환한다.")
    @Test
    void deleteOrder_success() throws Exception {
        mockMvc.perform(
                delete("/orders/{orderId}", "order-id")
        ).andExpect(status().isNoContent());

        verify(orderService).deleteOrderByOrderId("order-id");
    }
}
