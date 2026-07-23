package roomescape.reservationWaiting.controller;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.annotation.Authenticated;
import roomescape.auth.annotation.LoginName;
import roomescape.order.controller.dto.OrderResponse;
import roomescape.order.domain.Order;
import roomescape.reservationWaiting.controller.dto.ReservationWaitingRequest;
import roomescape.reservationWaiting.controller.dto.ReservationWaitingResponse;
import roomescape.reservationWaiting.domain.ReservationWaiting;
import roomescape.reservationWaiting.service.ReservationWaitingService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/reservation-waitings")
public class ReservationWaitingController {

    private final ReservationWaitingService reservationWaitingService;

    @PostMapping
    public ResponseEntity<ReservationWaitingResponse> createReservationWaiting(
            @RequestBody ReservationWaitingRequest request) {
        ReservationWaiting reservationWaiting = reservationWaitingService.makeReservationWaiting(request.toCommand());
        ReservationWaitingResponse response = ReservationWaitingResponse.from(reservationWaiting);

        return ResponseEntity
                .created(URI.create("/reservation-waitings/" + response.id()))
                .body(response);
    }

    @Authenticated
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyReservationWaiting(
            @LoginName String name, @PathVariable Long id) {
        reservationWaitingService.deleteReservationWaitingById(id, name);
        return ResponseEntity.noContent().build();
    }

    @Authenticated
    @GetMapping("/promotable")
    public ResponseEntity<List<ReservationWaitingResponse>> getMyPromotableReservationWaitings(
            @LoginName String name) {
        List<ReservationWaitingResponse> responses = reservationWaitingService.findPromotableWaitingsByName(name)
                .stream()
                .map(ReservationWaitingResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Authenticated
    @PostMapping("/{id}/promote")
    public ResponseEntity<OrderResponse> promoteMyReservationWaiting(@LoginName String name, @PathVariable Long id) {
        Order order = reservationWaitingService.promoteWaiting(id, name);

        return ResponseEntity.created(URI.create("/orders/" + order.getOrderId()))
                .body(OrderResponse.from(order));
    }
}
