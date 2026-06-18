package com.fih.companion.stats.dto;

import java.time.LocalDate;

public record EventRollupDto(
        int eventId,
        String title,
        LocalDate date,
        long scans,
        long accepted,
        long rejected,
        double acceptanceRate,
        long publicScans,
        long vipScans
) {
}
