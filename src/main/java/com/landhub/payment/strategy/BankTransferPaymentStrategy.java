package com.landhub.payment.strategy;

import com.landhub.payment.Payment;
import com.landhub.payment.PaymentMethod;
import com.landhub.payment.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class BankTransferPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod supports() {
        return PaymentMethod.BANK_TRANSFER;
    }

    @Override
    public void validate(Payment payment) {
        if (!hasText(payment.getBankReference())) {
            throw new IllegalArgumentException("Bank transfer reference is required.");
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
