package com.fih.companion.badge.dto;

import java.time.LocalDate;


public record MissingPosterDto(int eventId, String eventTitle, LocalDate eventDate, int invitationCount) {
}
