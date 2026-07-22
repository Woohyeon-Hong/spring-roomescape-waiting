package roomescape.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import roomescape.order.exception.OrderNotFoundException;
import roomescape.order.repository.OrderRepository;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public void deleteOrderByOrderId(String orderId) {
        int affectedRow = orderRepository.deleteByOrderId(orderId);
        int nonAffected = 0;

        if (affectedRow == nonAffected) {
            throw new OrderNotFoundException();
        }
    }
}
