package roomescape.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import roomescape.order.domain.Order;

@JdbcTest
class JdbcOrderRepositoryTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    OrderRepository orderRepository;

    @Autowired
    public JdbcOrderRepositoryTest(JdbcTemplate jdbcTemplate) {
        orderRepository = new JdbdcOrderRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("새로운 주문을 저장하고 반환된 객체의 ID를 확인한다.")
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
    @DisplayName("기존에 이미 같은 orderId의 주문이 있으면 예외가 발생한다.")
    void saveTest_duplicate() {
        // given
        Order order = Order.of(1000L);
        orderRepository.save(order);

        // when & then
        assertThatThrownBy(() -> orderRepository.save(new Order(null, order.getOrderId(), 2000L, false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("ID를 통해 저장된 주문 조회한다.")
    void findByIdTest() {
        // given
        Order saved = orderRepository.save(Order.of(1000L));

        // when
        Order found = orderRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("조회된 결과가 없습니다. id: " + saved.getId()));

        // then
        assertThat(saved).isEqualTo(found);
    }

    @DisplayName("orderId를 통해 저장된 주문 조회한다.")
    void findByOrderIdTest() {
        // given
        Order saved = orderRepository.save(Order.of(1000L));

        // when
        Order found = orderRepository.findByOrderId(saved.getOrderId())
                .orElseThrow(() -> new AssertionError("조회된 결과가 없습니다. orderId: " + saved.getId()));

        // then
        assertThat(saved).isEqualTo(found);
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
}
