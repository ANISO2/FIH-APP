package com.fih.companion.invitation.dto;

/**
 * One row in a lot preview: the matched invitation, the name it WOULD receive,
 * and whether it is already assigned (which would block the whole lot).
 */
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
