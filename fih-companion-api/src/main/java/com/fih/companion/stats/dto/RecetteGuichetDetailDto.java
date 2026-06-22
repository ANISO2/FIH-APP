package com.fih.companion.stats.dto;

import java.time.LocalDate;

/**
 * One row of "Recette par guichet — détail" (per event x model). The Billet group
 * mirrors the legacy guichet breakdown (confirmed against the schema):
 *   Livraison      = SUM(livraison.nbrebillets)  billets delivered to the guichet
 *   Vente          = SUM(vente.nombre)           billets sold at the guichet
 *   Prix Unitaire  = vente.montantunitaire       unit price charged at the guichet
 *   Recette        = SUM(vente.montantnet)       revenue
 *   Reste          = Livraison - Vente           unsold stock left at the guichet
 *   Kit            = kit revenue for this event x model (via detailkit -> kit)
 * Empty in this edition (no point-of-sale rows) → "Aucun enregistrement trouvé".
 */
public record RecetteGuichetDetailDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        int modelId,
        String modelName,
        long billetLivraison,
        long billetVente,
        double billetPrixUnitaire,
        double billetRecette,
        long billetReste,
        double kit
) {
}
