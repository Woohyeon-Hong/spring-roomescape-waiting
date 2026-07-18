package roomescape.order.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmRequest(
        @NotBlank(message = "paymentKey는 필수입니다.")
        String paymentKey,
        @NotNull(message = "결제 금액은 필수입니다.")
        Long amount
) {
}
