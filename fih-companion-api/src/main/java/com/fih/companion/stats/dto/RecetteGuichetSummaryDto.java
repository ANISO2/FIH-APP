package com.fih.companion.stats.dto;

import java.time.LocalDate;


public record RecetteGuichetSummaryDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        double billet,
        double kit,
        double total
) {
}
