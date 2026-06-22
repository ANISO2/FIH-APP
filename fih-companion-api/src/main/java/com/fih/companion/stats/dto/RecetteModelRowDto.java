package com.fih.companion.stats.dto;

/**
 * Recette détaillée — one per-model ROW inside an event panel (Change C),
 * loaded lazily when the panel is expanded. Quantities are grouped by category
 * (Billet then Voucher), each broken into Génération (stock), Vente (sold) and
 * Reste (stock - sold). Kit removed.
 *
 *   montant     = generation.prix (unit price for this event x model)
 *   billet*     = stockbillet / counterbillet / (stock - counter)
 *   voucher*    = stockvoucher / countervoucher / (stock - counter)
 *   totalVendu  = counterbillet + countervoucher
 *   recetteTnd  = totalVendu * montant
 *   tauxVente   = totalVendu / (billetGeneration + voucherGeneration) * 100
 */
public record RecetteModelRowDto(
        int modelId,
        String modelName,
        double montant,
        long billetGeneration,
        long billetVente,
        long billetReste,
        long voucherGeneration,
        long voucherVente,
        long voucherReste,
        long totalVendu,
        double recetteTnd,
        double tauxVente
) {
}
