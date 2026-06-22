package com.fih.companion.badge.projection;

import java.sql.Timestamp;

public interface BadgeItemProjection {
    String getType();
    String getNumeroserie();
    String getCodebarre();
    String getHolderName();
    /** Name from badge_affectation (null when none assigned). */
    String getAffecteeA();
    /**
     * §6 — when the badge was last printed (null when never printed).
     * Returned as java.sql.Timestamp (the raw native type, like
     * AvailabilityProjection.getEventDate()); the service converts to
     * LocalDateTime. This keeps native-projection mapping reliable.
     */
    Timestamp getPrintedAt();
}
