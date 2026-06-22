package com.fih.companion.invitation.projection;

/** One billet found inside a serial range, with its model/event and any existing name. */
public interface LotRowProjection {
    String getNumeroserie();
    String getCodebarre();
    Integer getEventId();
    String getEventTitle();
    Integer getModelId();
    String getModelName();
    /** Existing assigned name (null when not yet assigned). */
    String getAffecteeA();
}
