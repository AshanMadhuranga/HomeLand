package com.landhub.payment.strategy;

import com.landhub.payment.Payment;
import com.landhub.payment.PaymentMethod;

public interface PaymentStrategy {

    PaymentMethod supports();

    void validate(Payment payment);

    void initialize(Payment payment);
}
