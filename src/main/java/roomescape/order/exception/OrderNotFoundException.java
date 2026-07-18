package roomescape.order.exception;

import roomescape.global.exception.NotFoundException;

public class OrderNotFoundException extends NotFoundException {

    public OrderNotFoundException() {
        super("존재하지 않는 주문입니다.");
    }
}
