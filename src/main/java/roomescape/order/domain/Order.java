package roomescape.order.domain;

import java.util.Objects;
import java.util.UUID;
import roomescape.order.exception.InvalidAmountValueException;

public class Order {

    private final Long id;
    private final String orderId;
    private final Long amount;

    public Order(Long id, String orderId, Long amount) {
        if (amount < 0) {
            throw new InvalidAmountValueException();
        }

        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
    }

    public static Order of (Long amount) {
        return new Order(null, UUID.randomUUID().toString(), amount);
    }

    public Order updateId(Long id) {
        return new Order(id, this.orderId, this.amount);
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public Long getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order order)) {
            return false;
        }
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
