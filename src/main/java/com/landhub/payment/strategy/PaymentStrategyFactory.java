package com.landhub.payment.strategy;

import com.landhub.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentStrategyFactory {

    private final Map<PaymentMethod, PaymentStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
        this.strategies = new EnumMap<>(PaymentMethod.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.supports(), strategy));
    }

    public PaymentStrategy getStrategy(PaymentMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        PaymentStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("No payment strategy is configured for " + method + ".");
        }
        return strategy;
    }
}
