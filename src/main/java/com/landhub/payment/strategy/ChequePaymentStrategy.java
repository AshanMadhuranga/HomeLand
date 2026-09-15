package com.landhub.payment.strategy;

import com.landhub.payment.Payment;
import com.landhub.payment.PaymentMethod;
import com.landhub.payment.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class ChequePaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod supports() {
        return PaymentMethod.CHEQUE;
    }

    @Override
    public void validate(Payment payment) {
        if (!hasText(payment.getChequeNumber())) {
            throw new IllegalArgumentException("Cheque number is required.");
        }
    }

    @Override
    public void initialize(Payment payment) {
        payment.setStatus(PaymentStatus.PENDING);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
