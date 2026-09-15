package com.landhub.payment.strategy;

import com.landhub.payment.Payment;
import com.landhub.payment.PaymentMethod;
import com.landhub.payment.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class DemoCardPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod supports() {
        return PaymentMethod.DEMO_CARD_GATEWAY;
    }

    @Override
    public void validate(Payment payment) {
        // Demo gateway does not collect real card data in this university demo.
    }

    @Override
    public void initialize(Payment payment) {
        payment.setStatus(PaymentStatus.PENDING);
    }
}
