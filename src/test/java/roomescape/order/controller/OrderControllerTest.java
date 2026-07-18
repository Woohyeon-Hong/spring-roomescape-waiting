package roomescape.order.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import roomescape.order.domain.Order;
import roomescape.order.service.OrderService;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @DisplayName("금액을 받아 새로운 주문을 생성하고, 201을 반환한다.")
    @Test
    void createOrder_success() throws Exception {
        //given
        when(orderService.makeOrder(1000L))
                .thenReturn(new Order(1L, "order-id", 1000L));

        String body = """
                {
                    "amount": 1000
                }
                """;

        //when & then
        mockMvc.perform(
                post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isCreated());
    }

    @DisplayName("금액이 없으면 400을 반환한다.")
    @Test
    void createOrder_no_amount() throws Exception {
        //given
        String body = """
                {
                }
                """;

        //when & then
        mockMvc.perform(
                post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isBadRequest());
    }

    @DisplayName("금액이 0 이하이면 400을 반환한다.")
    @Test
    void createOrder_non_positive_amount() throws Exception {
        //given
        String body = """
                {
                    "amount": 0
                }
                """;

        //when & then
        mockMvc.perform(
                post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isBadRequest());
    }
}
