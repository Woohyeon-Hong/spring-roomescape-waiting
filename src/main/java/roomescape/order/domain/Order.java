package roomescape.order.domain;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import roomescape.order.exception.InvalidAmountValueException;
import roomescape.order.exception.InvalidPaymentKeyException;

@Getter
public class Order {

    private final Long id;
    private final String orderId;
    private final Long amount;
    private final String paymentKey;

    public Order(Long id, String orderId, Long amount, String paymentKey) {
        if (amount < 0) {
            throw new InvalidAmountValueException();
        }

        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentKey = paymentKey;
    }

    public static Order of(Long amount) {
        return new Order(null, UUID.randomUUID().toString(), amount, null);
    }

    public Order updateId(Long id) {
        return new Order(id, this.orderId, this.amount, this.paymentKey);
    }

    public Order updatePaymentKey(String paymentKey) {
        if (paymentKey == null) {
            throw new InvalidPaymentKeyException();
        }
        return new Order(this.id, this.orderId, this.amount, paymentKey);
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
