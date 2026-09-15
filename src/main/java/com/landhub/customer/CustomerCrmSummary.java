package com.landhub.customer;

import com.landhub.auth.User;

import java.math.BigDecimal;

public record CustomerCrmSummary(
        User customer,
        long inquiries,
        long siteVisits,
        long bookings,
        long completedBookings,
        BigDecimal totalPaid,
        long reviews,
        long feedback
) {
}
