package com.fih.companion.badge.dto;

public record BadgeItemDto(
        String type, String numeroserie, String codebarre, String holderName, String affecteeA, boolean hasPhoto
) {
}
