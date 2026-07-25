package roomescape.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.payment.controller.dto.PaymentConfirmRequest;
import roomescape.payment.service.PaymentService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirm(
            @PathVariable("orderId") String orderId,
            @RequestBody PaymentConfirmRequest request
    ) {
        paymentService.confirm(orderId, request.paymentKey(), request.amount());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{orderId}/fail")
    public ResponseEntity<Void> fail(@PathVariable("orderId") String orderId) {
        paymentService.rollback(orderId);
        return ResponseEntity.noContent().build();
    }
}
