package com.fih.companion.invitation.dto;

import java.util.List;

/**
 * Result of previewing a lot BEFORE assigning (read-only).
 *
 *  - eligibleCount        : invitation billets found in the range
 *  - alreadyAssignedCount : how many of those already have a name (any > 0 blocks)
 *  - nonInvitationCount   : non-invitation billets found in the range (ignored,
 *                           just a warning so the admin can tighten the range)
 *  - baseNameAlreadyUsed  : §6 — this base name (or baseName-NN) already exists
 *  - canAssign            : true only when there is ≥1 eligible and none assigned
 *  - items                : the eligible rows with their proposed names
 *  - alreadyAssignedSerials: serials to show when the lot is blocked
 */
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
