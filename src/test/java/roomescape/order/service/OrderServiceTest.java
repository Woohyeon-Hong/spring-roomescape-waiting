package roomescape.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import roomescape.order.exception.OrderNotFoundException;
import roomescape.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService orderService;

    @DisplayName("orderId로 주문을 삭제한다.")
    @Test
    void deleteOrderByOrderIdTest_success() {
        //given
        when(orderRepository.deleteByOrderId("order-id"))
                .thenReturn(1);

        //when
        orderService.deleteOrderByOrderId("order-id");

        //then
        verify(orderRepository).deleteByOrderId("order-id");
    }

    @DisplayName("주문이 없으면 예외가 발생한다.")
    @Test
    void deleteOrderByOrderIdTest_not_found() {
        //when & then
        assertThatThrownBy(() -> orderService.deleteOrderByOrderId("order-id"))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository).deleteByOrderId("order-id");
    }
}
