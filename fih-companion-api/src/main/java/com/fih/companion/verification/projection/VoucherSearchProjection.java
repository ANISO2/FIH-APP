package com.fih.companion.verification.projection;

/**
 * Closed interface projection for the backoffice "Vérification Voucher" list
 * (3.2). Returns ONLY the columns shown in the voucher table mockup. Read-only;
 * no entity/relation eager-loading. Getters map to the quoted aliases in the
 * native search query.
 */
public interface VoucherSearchProjection {
    String getEventTitle();    // evenement.titre (Spectacle)
    String getModelName();     // modelebillet.modele (Modèle)
    String getNumeroserie();
    String getCodebarre();
    Boolean getUtilisation();
    Boolean getVendu();        // Vente
    Boolean getActivation();   // Activé
    Boolean getReservation();
    String getCommande();      // voucherorder.code
}
