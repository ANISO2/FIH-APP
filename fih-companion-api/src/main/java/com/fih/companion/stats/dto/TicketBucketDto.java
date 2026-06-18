package com.fih.companion.stats.dto;

/** Issued vs scanned for one ticket type. scanned = distinct tickets seen in the log. */
public record TicketBucketDto(long issued, long scanned) {
}
