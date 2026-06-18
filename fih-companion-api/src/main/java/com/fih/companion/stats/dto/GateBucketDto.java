package com.fih.companion.stats.dto;

/** Scan counts for one gate. */
public record GateBucketDto(long scans, long accepted, long rejected) {
}
