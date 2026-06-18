package com.fih.companion.stats.dto;

import java.time.LocalDate;
import java.util.List;

public record EventDetailDto(
        int eventId,
        String title,
        LocalDate date,
        long scans,
        long accepted,
        long rejected,
        double acceptanceRate,
        GateDto gate,
        List<HourEntryDto> entriesByHour
) {
}
