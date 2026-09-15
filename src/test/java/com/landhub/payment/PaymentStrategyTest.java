package com.landhub.payment;

import com.landhub.payment.strategy.BankTransferPaymentStrategy;
import com.landhub.payment.strategy.CashPaymentStrategy;
import com.landhub.payment.strategy.ChequePaymentStrategy;
import com.landhub.payment.strategy.DemoCardPaymentStrategy;
import com.landhub.payment.strategy.PaymentStrategy;
import com.landhub.payment.strategy.PaymentStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentStrategyTest {

    private PaymentStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PaymentStrategyFactory(List.of(
                new DemoCardPaymentStrategy(),
                new BankTransferPaymentStrategy(),
                new CashPaymentStrategy(),
                new ChequePaymentStrategy()
        ));
    }

    @Test
    void factoryReturnsDemoCardStrategy() {
        assertInstanceOf(DemoCardPaymentStrategy.class, factory.getStrategy(PaymentMethod.DEMO_CARD_GATEWAY));
    }

    @Test
    void factoryReturnsBankTransferStrategy() {
        assertInstanceOf(BankTransferPaymentStrategy.class, factory.getStrategy(PaymentMethod.BANK_TRANSFER));
    }

    @Test
    void factoryReturnsCashStrategy() {
        assertInstanceOf(CashPaymentStrategy.class, factory.getStrategy(PaymentMethod.CASH));
    }

    @Test
    void factoryReturnsChequeStrategy() {
        assertInstanceOf(ChequePaymentStrategy.class, factory.getStrategy(PaymentMethod.CHEQUE));
    }

    @Test
    void bankTransferRejectsMissingReference() {
        Payment payment = payment(PaymentMethod.BANK_TRANSFER);
        PaymentStrategy strategy = factory.getStrategy(PaymentMethod.BANK_TRANSFER);

        assertThrows(IllegalArgumentException.class, () -> strategy.validate(payment));
    }

    @Test
    void chequeRejectsMissingChequeNumber() {
        Payment payment = payment(PaymentMethod.CHEQUE);
        PaymentStrategy strategy = factory.getStrategy(PaymentMethod.CHEQUE);

        assertThrows(IllegalArgumentException.class, () -> strategy.validate(payment));
    }

    @Test
    void validBankTransferInitializesPending() {
        Payment payment = payment(PaymentMethod.BANK_TRANSFER);
        payment.setBankReference("BANK-123");
        PaymentStrategy strategy = factory.getStrategy(PaymentMethod.BANK_TRANSFER);

        strategy.validate(payment);
        strategy.initialize(payment);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void validChequeInitializesPending() {
        Payment payment = payment(PaymentMethod.CHEQUE);
        payment.setChequeNumber("CHQ-123");
        PaymentStrategy strategy = factory.getStrategy(PaymentMethod.CHEQUE);

        strategy.validate(payment);
        strategy.initialize(payment);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void demoCardInitializesPending() {
        Payment payment = payment(PaymentMethod.DEMO_CARD_GATEWAY);
        PaymentStrategy strategy = factory.getStrategy(PaymentMethod.DEMO_CARD_GATEWAY);

        strategy.validate(payment);
        strategy.initialize(payment);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void cashInitializesPending() {
        Payment payment = payment(PaymentMethod.CASH);
        PaymentStrategy strategy = factory.getStrategy(PaymentMethod.CASH);

        strategy.validate(payment);
        strategy.initialize(payment);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    private Payment payment(PaymentMethod method) {
        Payment payment = new Payment();
        payment.setPaymentMethod(method);
        return payment;
    }
}
