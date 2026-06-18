package com.fih.companion.stats.dto;

import java.time.LocalDate;

public record EntryByDayDto(LocalDate date, long scans, long accepted, long rejected) {
}
