package com.fih.companion.badge.projection;

public interface BadgeItemProjection {
    String getType();
    String getNumeroserie();
    String getCodebarre();
    String getHolderName();
    /** Name from badge_affectation (null when none assigned). */
    String getAffecteeA();
}
