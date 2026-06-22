package com.fih.companion.badge.dto;

import java.time.LocalDate;

/**
 * §6 — one event that has invitations but no poster file on disk yet.
 * Powers the "affiches manquantes" quick list in the backoffice.
 */
public record MissingPosterDto(int eventId, String eventTitle, LocalDate eventDate, int invitationCount) {
}
