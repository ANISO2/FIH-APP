package com.fih.companion.badge.dto;

import java.time.LocalDate;
import java.util.List;

/** How many invitation/badge records exist for one event x ticket-model. */
public record AvailabilityDto(
        int eventId, String eventTitle, LocalDate eventDate,
        int modelId, String modelName, List<String> accessZones,
        int injectedCount, int billetCount, int voucherCount,
        Integer withPhotoCount,     // null unless photo check was requested
        Integer missingPhotoCount
) {
}
