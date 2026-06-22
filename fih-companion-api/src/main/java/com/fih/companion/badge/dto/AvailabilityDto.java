package com.fih.companion.badge.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * How many invitation/badge records exist for one event x ticket-model.
 *
 * Change A: the old per-ticket photo counts (withPhotoCount / missingPhotoCount)
 * are gone. Coverage is now a single per-EVENT flag — does this event have a
 * poster file on disk? The same value repeats across every model row of the same
 * event (the poster belongs to the event, not the model).
 */
public record AvailabilityDto(
        int eventId, String eventTitle, LocalDate eventDate,
        int modelId, String modelName, List<String> accessZones,
        int injectedCount, int billetCount, int voucherCount,
        boolean eventHasPoster
) {
}
