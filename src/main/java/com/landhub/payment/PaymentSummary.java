package com.landhub.payment;

import com.landhub.booking.Booking;

import java.math.BigDecimal;

public record PaymentSummary(
        Booking booking,
        BigDecimal bookingPrice,
        BigDecimal totalPaid,
        BigDecimal remainingBalance
) {
}
