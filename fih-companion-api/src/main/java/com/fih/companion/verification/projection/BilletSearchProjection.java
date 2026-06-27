package com.fih.companion.verification.projection;

import java.time.LocalDate;

/**
 * Closed interface projection for the backoffice "Vérification Billet" list
 * (3.2). Returns ONLY the columns shown in the billet table mockup — no entity
 * or relation is eager-loaded. Each getter maps to the matching quoted alias in
 * the native search query. Read-only.
 */
public interface BilletSearchProjection {
    String getNumeroserie();
    String getCodebarre();
    Boolean getActivation();
    Boolean getLivre();        // billet.etatlivraison
    Boolean getVendu();
    Boolean getUtilise();      // billet.utilisation
    String getEventTitle();    // evenement.titre
    String getModelName();     // modelebillet.modele
    LocalDate getDateVente();  // vente.datevente (null until sold at a guichet)
    String getLivreur();       // livreur.rolecontroleur via livraison.controlleur
    LocalDate getDateLivraison(); // livraison.datelivraison
}
