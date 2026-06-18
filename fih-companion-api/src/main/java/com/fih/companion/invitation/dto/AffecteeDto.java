package com.fih.companion.invitation.dto;

import java.time.LocalDateTime;

/** What we return after reading or setting the name for an invitation. */
public record AffecteeDto(
        String numeroserie,
        String affecteeA,
        LocalDateTime updatedAt,
        String updatedBy
) {
}
