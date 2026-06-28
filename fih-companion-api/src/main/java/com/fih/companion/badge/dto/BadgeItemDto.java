package com.fih.companion.badge.dto;

import java.time.LocalDateTime;


public record BadgeItemDto(
        String type, String numeroserie, String codebarre,
        String holderName, String affecteeA, LocalDateTime printedAt
) {
}
