package com.fih.companion.badge.dto;

import java.util.List;

 public record BatchRequest(Integer eventId, Integer modelId, List<String> codes) {
}
