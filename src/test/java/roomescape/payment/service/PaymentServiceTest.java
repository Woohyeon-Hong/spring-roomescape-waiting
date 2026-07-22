package roomescape.payment.service;

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
import roomescape.order.exception.InvalidPaymentKeyException;
import roomescape.order.exception.OrderNotFoundException;
import roomescape.order.exception.PaymentAmountMismatchException;
import roomescape.order.repository.OrderRepository;
import roomescape.payment.PaymentConfirmation;
import roomescape.payment.PaymentGateway;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    PaymentGateway paymentGateway;

    @Mock
    PaymentConfirmationApplier paymentConfirmationApplier;

    @InjectMocks
    PaymentService paymentService;

    @DisplayName("결제 승인 요청을 하면, 토스 승인 API를 호출하고 주문/예약에 결과를 반영한다.")
    @Test
    void confirmTest_success() {
        //given
        Order order = new Order(1L, "order-id", 1000L, null);
        when(orderRepository.findByOrderId("order-id"))
                .thenReturn(Optional.of(order));

        //when
        paymentService.confirm("order-id", "payment-key", 1000L);

        //then
        verify(paymentGateway).confirm(new PaymentConfirmation("payment-key", "order-id", 1000L));
        verify(paymentConfirmationApplier).apply(order, "payment-key");
    }

    @DisplayName("주문이 없으면 예외가 발생한다.")
    @Test
    void confirmTest_paymentKey_is_null() {
        assertThatThrownBy(() -> paymentService.confirm("order-id", null, 1000L))
                .isInstanceOf(InvalidPaymentKeyException.class);
    }

    @DisplayName("주문이 없으면 예외가 발생한다.")
    @Test
    void confirmTest_order_not_found() {
        //given
        when(orderRepository.findByOrderId("no-such-order")).thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> paymentService.confirm("no-such-order", "payment-key", 1000L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @DisplayName("콜백으로 넘어온 금액이 저장된 주문 금액과 다르면 PG 사의 승인 API 호출 전에 예외가 발생한다.")
    @Test
    void confirmTest_amount_mismatch() {
        //given
        when(orderRepository.findByOrderId("order-id"))
                .thenReturn(Optional.of(new Order(1L, "order-id", 1000L, null)));

        //when & then
        assertThatThrownBy(() -> paymentService.confirm("order-id", "payment-key", 500L))
                .isInstanceOf(PaymentAmountMismatchException.class);

        verify(paymentGateway, never()).confirm(any());
        verify(paymentConfirmationApplier, never()).apply(any(), any());
    }
}
