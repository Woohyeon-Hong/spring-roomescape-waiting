package roomescape.reservation.controller.dto;

import java.time.LocalDate;
import roomescape.order.controller.dto.OrderResponse;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.theme.controller.dto.ThemeResponse;
import roomescape.time.controller.dto.ReservationTimeResponse;

public record ReservationResponse(
        Long id,
        String name,
        LocalDate date,
        ReservationStatus status,
        ReservationTimeResponse time,
        ThemeResponse theme,
        OrderResponse orderResponse
) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getName(),
                reservation.getDate(),
                reservation.getStatus(),
                ReservationTimeResponse.from(reservation.getReservationTime()),
                ThemeResponse.from(reservation.getTheme()),
                OrderResponse.from(reservation.getOrder())
        );
    }
}
