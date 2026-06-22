package com.fih.companion.stats.dto;

import java.time.LocalDate;

/**
 * One row of the "Recette résumé" table: revenue (TND) per event, split by
 * category. Mirrors the legacy report meaning where the summary shows MONEY,
 * not counts. The grand-total row is computed in the backoffice by summing
 * these rows, so it always stays consistent regardless of client-side sorting.
 *
 * Kit removed (Change B): counterkit is 0 across the data and had no real
 * "stock" source, so the column was pure noise. All amounts are
 * revenue = soldQuantity * generation.prix, summed over the event's models.
 */
public record RecetteSummaryDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        double billet,   // revenue TND from billets  (sum of counterbillet  * prix)
        double voucher,  // revenue TND from vouchers (sum of countervoucher * prix)
        double total     // billet + voucher
) {
}
