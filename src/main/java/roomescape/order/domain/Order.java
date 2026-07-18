package roomescape.order.domain;

import java.util.Objects;
import java.util.UUID;
import roomescape.order.exception.InvalidAmountValueException;

public class Order {

    private final Long id;
    private final String orderId;
    private final Long amount;
    private final boolean isConfirmed;

    public Order(Long id, String orderId, Long amount, boolean isConfirmed) {
        if (amount < 0) {
            throw new InvalidAmountValueException();
        }

        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.isConfirmed = isConfirmed;
    }

    public static Order of (Long amount) {
        return new Order(null, UUID.randomUUID().toString(), amount, false);
    }

    public Order updateId(Long id) {
        return new Order(id, this.orderId, this.amount, this.isConfirmed);
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

    public boolean isConfirmed() {
        return isConfirmed;
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
