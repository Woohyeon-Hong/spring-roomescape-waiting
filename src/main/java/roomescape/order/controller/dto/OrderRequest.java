package roomescape.order.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotNull(message = "주문 금액은 필수입니다.")
        @Positive(message = "주문 금액은 0보다 커야 합니다.")
        Long amount
) {
}
