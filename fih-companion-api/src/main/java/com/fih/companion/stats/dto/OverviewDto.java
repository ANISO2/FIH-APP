package com.fih.companion.stats.dto;

import java.time.LocalDate;

/** Global KPIs for the top of the dashboard. */
public record OverviewDto(
        long totalEvents,
        long totalBillets,
        long totalVouchers,
        long totalScans,
        long acceptedScans,
        long rejectedScans,
        double acceptanceRate,   // 0..100, one decimal
        long publicScans,
        long vipScans,
        String busiestEventTitle,
        LocalDate busiestEventDate,
        long busiestEventScans
) {
}
