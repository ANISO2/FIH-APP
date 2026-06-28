package com.fih.companion.verification.projection;

import java.time.LocalDate;


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
