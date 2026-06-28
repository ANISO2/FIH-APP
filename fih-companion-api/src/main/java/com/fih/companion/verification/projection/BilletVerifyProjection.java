package com.fih.companion.verification.projection;

import java.time.LocalDate;

public interface BilletVerifyProjection {
    String getNumeroserie();
    String getCodebarre();
    Integer getModelId();        // billet.modelebillet (for access-zone resolving)
    String getEventTitle();
    LocalDate getEventDate();
    String getModelName();
    Integer getMaxAccess();      // modelebillet.maxaccess (null if no model row)
    Boolean getActivation();
    Boolean getUtilisation();
    Boolean getVendu();
    Boolean getReservation();
    Integer getNombreacces();    // live use counter
    String getHolderName();      // "firstname lastname" or null
    String getAffecteeA();       // assigned name from our badge_affectation, or null
}
