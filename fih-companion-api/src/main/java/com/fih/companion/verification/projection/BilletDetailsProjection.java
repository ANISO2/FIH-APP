package com.fih.companion.verification.projection;

import java.time.LocalDate;

/** Management extras for a billet, loaded lazily on the details screen. */
public interface BilletDetailsProjection {
    String getNumeroserie();
    String getCodebarre();
    Boolean getLivre();          // etatlivraison
    LocalDate getDateLivraison(); // livraison.datelivraison
    LocalDate getDateVente();     // vente.datevente
}
