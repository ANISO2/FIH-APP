package com.fih.companion.invitation.dto;

import java.util.List;

/** What we return after a successful lot assignment. */
public record LotResultDto(int assignedCount, List<AffecteeDto> assigned) {
}
