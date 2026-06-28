package com.fih.companion.invitation.dto;


public record LotItemDto(
        String numeroserie,
        String codebarre,
        Integer eventId,
        String eventTitle,
        Integer modelId,
        String modelName,
        String proposedName,
        boolean alreadyAssigned,
        String existingName
) {
}
