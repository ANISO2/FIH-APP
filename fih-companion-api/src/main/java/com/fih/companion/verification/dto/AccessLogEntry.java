package com.fih.companion.verification.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One access-log line for the mobile details screen. */
public record AccessLogEntry(
        Integer reference,
        String codebarre,
        LocalDate date,
        LocalDateTime time,
        String porte,
        boolean granted   // transactionstate
) {
}
