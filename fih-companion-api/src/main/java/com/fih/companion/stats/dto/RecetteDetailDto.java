package com.fih.companion.stats.dto;

import java.time.LocalDate;

/**
 * One row of the "Recette détaillée" table: a single (event x model) generation.
 * Quantities are grouped by category (Voucher / Billet / Kit), each broken into
 * Génération (stock), Vente (sold), Reste (stock - sold).
 *
 * Column sources (confirmed against the real `generation` table):
 *   Montant       = generation.prix (unit price for this event x model)
 *   Voucher Gén.  = stockvoucher      Vente = countervoucher   Reste = stock - vente
 *   Billet  Gén.  = stockbillet       Vente = counterbillet    Reste = stock - vente
 *   Kit     Gén.  = counterkit *      Vente = counterkit       Reste = 0
 *   Total         = counterbillet + countervoucher + counterkit (sold across categories)
 *   Recette TND   = Total * Montant
 *
 * (*) The `generation` table has NO `stockkit` column — only `counterkit`. So
 *     for kits we mirror counterkit into both Génération and Vente (Reste = 0),
 *     which avoids a negative "Reste". In this dev edition counterkit is 0
 *     everywhere, so kit columns are all zero; the mapping matters only for
 *     future editions that actually sell kits. See RecetteDetailProjection.
 */
public record RecetteDetailDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        int modelId,
        String modelName,
        double montant,
        long voucherGeneration,
        long voucherVente,
        long voucherReste,
        long billetGeneration,
        long billetVente,
        long billetReste,
        long kitGeneration,
        long kitVente,
        long kitReste,
        long total,
        double recetteTnd
) {
}
