package roomescape.reservationWaiting.controller.dto;

import roomescape.reservation.exception.InvalidReservationRequestFormatException;

public record ReservationWaitingPromoteRequest(String orderId) {

    public ReservationWaitingPromoteRequest {
        if (orderId == null || orderId.isBlank()) {
            throw new InvalidReservationRequestFormatException();
        }
    }
}
