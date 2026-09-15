package com.landhub.customer;

import java.time.LocalDateTime;

public record CustomerInteractionItem(
        LocalDateTime dateTime,
        String type,
        String title,
        String status,
        Long relatedId
) {
}
