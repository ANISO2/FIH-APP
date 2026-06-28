package com.fih.companion.invitation.dto;

import java.time.LocalDateTime;

 public record AffecteeDto(
        String numeroserie,
        String affecteeA,
        LocalDateTime updatedAt,
        String updatedBy,
        LocalDateTime printedAt
) {
}
