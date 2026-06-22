package com.fih.companion.stats.dto;

import java.time.LocalDate;

/**
 * One row of "Recette par guichet — résumé": box-office (guichet) revenue per
 * event, split Billet / Kit.
 *
 * Legacy meaning (confirmed against the schema). Unlike "Recette" — which is
 * derived from generation counters — the guichet report is driven by the
 * point-of-sale tables:
 *   Billet = SUM(vente.montantnet) for the event's guichet sales
 *   Kit    = SUM(kit.montantnet) linked to the event via detailkit
 *   Total  = Billet + Kit
 * The `vente`, `livraison` and `kit` tables are empty in this edition, so the
 * report is legitimately empty ("Aucun enregistrement trouvé"); the structure is
 * ready for editions that record guichet activity.
 */
public record RecetteGuichetSummaryDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        double billet,
        double kit,
        double total
) {
}
