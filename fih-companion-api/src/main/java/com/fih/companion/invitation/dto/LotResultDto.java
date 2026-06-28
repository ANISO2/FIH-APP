package com.fih.companion.invitation.dto;

import java.util.List;

 public record LotResultDto(int assignedCount, List<AffecteeDto> assigned) {
}
