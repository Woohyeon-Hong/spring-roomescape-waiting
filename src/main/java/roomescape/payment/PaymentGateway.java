package roomescape.payment;

public interface PaymentGateway {

    void confirm(PaymentConfirmation confirmation);
}
