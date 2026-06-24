package com.fih.companion.verification.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Everything a voucher verification needs, fetched in ONE native query
 * (voucher + modelebillet + evenement + badge_affectation). Same rationale as
 * {@link BilletVerifyProjection}: one round trip instead of several, and the
 * verdict is still computed live from the raw state below (never cached).
 */
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
