package com.fih.companion.badge.dto;

import java.util.List;

/** Body of POST /api/badges/batch. codes is optional (null/empty = all records). */
public record BatchRequest(Integer eventId, Integer modelId, List<String> codes) {
}
