package com.fih.companion.badge.dto;

import java.util.List;

/** Minimal page wrapper for paginated lists. */
public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
