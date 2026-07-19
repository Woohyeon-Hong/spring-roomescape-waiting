package roomescape.order.exception;

import roomescape.global.exception.DeleteFailedException;

public class OrderAlreadyConfirmedException extends DeleteFailedException {

    public OrderAlreadyConfirmedException() {
        super("이미 결제가 확정된 주문은 삭제할 수 없습니다.");
    }
}