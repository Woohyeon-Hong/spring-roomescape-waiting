package roomescape.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import roomescape.order.domain.Order;
import roomescape.order.exception.OrderNotFoundException;
import roomescape.order.exception.PaymentAmountMismatchException;
import roomescape.order.repository.OrderRepository;
import roomescape.payment.PaymentConfirmation;
import roomescape.payment.PaymentGateway;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    PaymentGateway paymentGateway;

    @InjectMocks
    OrderService orderService;

    @DisplayName("주문이 없으면 예외가 발생한다.")
    @Test
    void confirm_order_not_found() {
        //given
        when(orderRepository.findByOrderId("no-such-order")).thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> orderService.confirm("no-such-order", "payment-key", 1000L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @DisplayName("콜백으로 넘어온 금액이 저장된 주문 금액과 다르면 승인 API 호출 전에 예외가 발생한다.")
    @Test
    void confirm_amount_mismatch() {
        //given
        when(orderRepository.findByOrderId("order-id"))
                .thenReturn(Optional.of(new Order(1L, "order-id", 1000L, false)));

        //when & then
        assertThatThrownBy(() -> orderService.confirm("order-id", "payment-key", 500L))
                .isInstanceOf(PaymentAmountMismatchException.class);

        verify(paymentGateway, never()).confirm(any());
    }

    @DisplayName("금액이 일치하면 토스 승인 API를 호출하고, 성공하면 결제 키를 저장하며 주문을 확정한다.")
    @Test
    void confirm_success() {
        //given
        when(orderRepository.findByOrderId("order-id"))
                .thenReturn(Optional.of(new Order(1L, "order-id", 1000L, false)));

        //when
        orderService.confirm("order-id", "payment-key", 1000L);

        //then
        verify(paymentGateway).confirm(new PaymentConfirmation("payment-key", "order-id", 1000L));
        verify(orderRepository).confirmByOrderId("order-id", "payment-key");
    }
}
