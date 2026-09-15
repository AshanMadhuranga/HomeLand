package com.landhub.payment.strategy;

import com.landhub.payment.Payment;
import com.landhub.payment.PaymentMethod;
import com.landhub.payment.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class CashPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod supports() {
        return PaymentMethod.CASH;
    }

    @Override
    public void validate(Payment payment) {
        // Cash has no additional method-specific fields before admin confirmation.
    }

    @Override
    public void initialize(Payment payment) {
        payment.setStatus(PaymentStatus.PENDING);
    }
}
