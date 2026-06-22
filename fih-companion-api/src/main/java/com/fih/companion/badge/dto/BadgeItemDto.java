package com.fih.companion.badge.dto;

import java.time.LocalDateTime;

/**
 * Change A: the per-ticket "hasPhoto" flag is gone (coverage is now per-event
 * poster). §6: printedAt lets the UI show "affecté" vs "imprimé".
 */
public record BadgeItemDto(
        String type, String numeroserie, String codebarre,
        String holderName, String affecteeA, LocalDateTime printedAt
) {
}
