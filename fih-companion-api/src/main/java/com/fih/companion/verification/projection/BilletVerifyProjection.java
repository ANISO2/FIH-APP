package com.fih.companion.verification.projection;

import java.time.LocalDate;

/**
 * Everything a billet verification needs, fetched in ONE native query
 * (billet + modelebillet + evenement + holder + badge_affectation). Replaces the
 * 5–6 separate round trips the service used to make — the single highest-impact
 * change for surviving the gate-opening spike, since under load it is the number
 * of round trips × concurrency that saturates the (shared) database and pool.
 *
 * IMPORTANT: this carries only the raw state needed to DECIDE the verdict
 * (activation / utilisation / nombreacces) — it never carries a cached verdict.
 * The verdict is computed fresh in Java on every scan, so a ticket used a second
 * ago reads as ALREADY_USED. Reference fields (model/event labels) are static and
 * safe; the live counters are read straight from the row, every time.
 */
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
