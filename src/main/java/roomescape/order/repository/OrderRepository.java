package roomescape.order.repository;

import java.util.Optional;
import roomescape.order.domain.Order;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    Optional<Order> findByOrderId(String orderId);

    Optional<Order> findByOrderIdForUpdate(String orderId);

    void update(Order order);

    int deleteByOrderId(String orderId);
}
