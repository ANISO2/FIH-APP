package com.fih.companion.verification.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;


public interface VoucherVerifyProjection {
    String getNumeroserie();
    String getCodebarre();
    Integer getModelId();
    String getEventTitle();
    LocalDate getEventDate();
    String getModelName();
    Integer getMaxAccess();
    Boolean getActivation();
    Boolean getUtilisation();
    Boolean getVendu();
    Boolean getReservation();
    Integer getAccesscounter();      // live use counter
    LocalDateTime getDateannulation(); // non-null => cancelled
    String getAffecteeA();
}
