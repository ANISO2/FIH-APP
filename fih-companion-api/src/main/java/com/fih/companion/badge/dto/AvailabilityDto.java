package com.fih.companion.badge.dto;

import java.time.LocalDate;
import java.util.List;


public record AvailabilityDto(
        int eventId, String eventTitle, LocalDate eventDate,
        int modelId, String modelName, List<String> accessZones,
        int injectedCount, int billetCount, int voucherCount,
        boolean eventHasPoster
) {
}
