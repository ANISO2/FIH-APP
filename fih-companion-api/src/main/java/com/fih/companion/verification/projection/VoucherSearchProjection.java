package com.fih.companion.verification.projection;


public interface VoucherSearchProjection {
    String getEventTitle();    // evenement.titre (Spectacle)
    String getModelName();     // modelebillet.modele (Modèle)
    String getNumeroserie();
    String getCodebarre();
    Boolean getUtilisation();
    Boolean getVendu();        // Vente
    Boolean getActivation();   // Activé
    Boolean getReservation();
    Integer getCommande();   // voucherorder.code (integer in DB)
}
