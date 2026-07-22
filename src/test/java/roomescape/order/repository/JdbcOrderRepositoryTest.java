package roomescape.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import roomescape.order.domain.Order;
import roomescape.support.RepositoryTest;

class JdbcOrderRepositoryTest extends RepositoryTest {

    OrderRepository orderRepository;

    @Autowired
    public JdbcOrderRepositoryTest(JdbcTemplate jdbcTemplate) {
        orderRepository = new JdbcOrderRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("새로운 주문을 저장하고, 주문의 id를 반환한다.")
    void saveTest() {
        // given
        Order order = Order.of(1000L);

        // when
        Order saved = orderRepository.save(order);

        //then
        assertAll(
                () -> assertThat(saved.getId()).isNotNull(),
                () -> assertThat(saved.getAmount()).isEqualTo(1000L)
        );
    }

    @Test
    @DisplayName("주문 저장 시, 기존에 이미 같은 주문이 있으면 예외가 발생한다.")
    void saveTest_duplicate() {
        // given
        Order order = Order.of(1000L);
        orderRepository.save(order);

        // when & then
        assertThatThrownBy(() -> orderRepository.save(order))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("주문 저장 시, 기존에 이미 같은 orderId의 주문이 있으면 예외가 발생한다.")
    void saveTest_duplicate_order_id() {
        // given
        Order order = Order.of(1000L);
        orderRepository.save(order);

        // when & then
        assertThatThrownBy(() -> orderRepository.save(new Order(null, order.getOrderId(), 2000L, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("id를 통해 저장된 주문을 조회한다.")
    @Test
    void findByIdTest() {
        // given
        Order saved = orderRepository.save(Order.of(1000L));

        // when
        Order found = orderRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("조회된 결과가 없습니다. id: " + saved.getId()));

        // then
        assertThat(saved).isEqualTo(found);
    }

    @DisplayName("orderId를 통해 저장된 주문을 조회한다.")
    @Test
    void findByOrderIdTest() {
        // given
        Order saved = orderRepository.save(Order.of(1000L));

        // when
        Order found = orderRepository.findByOrderId(saved.getOrderId())
                .orElseThrow(() -> new AssertionError("조회된 결과가 없습니다. orderId: " + saved.getId()));

        // then
        assertThat(saved).isEqualTo(found);
    }

    @DisplayName("주문을 업데이트한다.")
    @Test
    void updateTest() {
        //given
        Order saved = orderRepository.save(Order.of(1000L));

        //when
        orderRepository.update(saved.updatePaymentKey("paymentkey"));

        //then
        Optional<Order> found = orderRepository.findByOrderId(saved.getOrderId());
        assertAll(
                () -> assertThat(found).isPresent(),
                () -> assertThat(found.get().getPaymentKey()).isEqualTo("paymentkey")
        );
    }

    @DisplayName("orderId를 통해 주문을 삭제하고, 삭제된 row 수를 반환한다.")
    @Test
    void deleteByOrderIdTest() {
        //given
        Order saved = orderRepository.save(Order.of(1000L));

        //when & then
        assertThat(orderRepository.deleteByOrderId(saved.getOrderId()))
                .isEqualTo(1);
    }

    @DisplayName("주문 삭제 시, orderId가 사용 중이면 예외가 발생한다.")
    @Test
    void deleteByOrderIdTest_in_use() {
        //given
        Order saved = orderRepository.save(Order.of(1000L));

        createReservation(
                "brown",
                createTime(LocalTime.of(10, 0)),
                LocalDate.of(2026, 5, 13),
                createTheme("테마", "설명", "url", 1000L),
                saved
        );

        //when & then
        assertThatThrownBy(() -> orderRepository.deleteByOrderId(saved.getOrderId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
