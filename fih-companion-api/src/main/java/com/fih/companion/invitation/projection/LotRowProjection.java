package com.fih.companion.invitation.projection;

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
