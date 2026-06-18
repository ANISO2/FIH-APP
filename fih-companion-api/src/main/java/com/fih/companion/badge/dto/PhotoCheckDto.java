package com.fih.companion.badge.dto;

import java.util.List;

public record PhotoCheckDto(int total, int withPhoto, int missing, List<String> missingCodes) {
}
