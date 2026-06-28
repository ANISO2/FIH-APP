package com.fih.companion.invitation.dto;

import java.util.List;


public record LotPreviewDto(
        int eligibleCount,
        int alreadyAssignedCount,
        int nonInvitationCount,
        boolean baseNameAlreadyUsed,
        boolean canAssign,
        List<LotItemDto> items,
        List<String> alreadyAssignedSerials
) {
}
