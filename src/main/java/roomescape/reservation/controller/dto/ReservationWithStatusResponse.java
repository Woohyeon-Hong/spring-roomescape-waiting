package roomescape.reservation.controller.dto;

import java.time.LocalDate;
import roomescape.reservation.service.dto.ReservationWithStatusResult;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

public record ReservationWithStatusResponse(
        Long id,
        String name,
        LocalDate date,
        ReservationTime time,
        Theme theme,
        String status,
        String orderId,
        Long amount,
        String paymentKey,
        Long waitingOrder
) {

    public static ReservationWithStatusResponse from (ReservationWithStatusResult result) {
        return new ReservationWithStatusResponse(
                result.id(),
                result.name(),
                result.date(),
                result.time(),
                result.theme(),
                result.status(),
                result.orderId(),
                result.amount(),
                result.paymentKey(),
                result.waitingOrder()
        );
    }
}
