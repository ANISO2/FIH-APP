package com.fih.companion.stats.dto;

import java.time.LocalDate;

/**
 * Recette détaillée — one collapsible panel HEADER (Change C). Holds the
 * per-event totals shown on the panel bar, modelled on the "Statistique des
 * tourniquets" layout. The per-model breakdown is loaded separately, on expand
 * (see RecetteModelRowDto), so the détaillée never returns thousands of rows.
 *
 *   totalGenere   = SUM(stockbillet + stockvoucher)        over the event's models
 *   totalVendu    = SUM(counterbillet + countervoucher)
 *   totalReste    = totalGenere - totalVendu
 *   recetteTotale = SUM((counterbillet + countervoucher) * prix)   [Kit excluded]
 *   tauxVente     = totalVendu / totalGenere * 100  (0..100, one decimal)
 */
public record RecetteEventHeaderDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        long totalGenere,
        long totalVendu,
        long totalReste,
        double recetteTotale,
        double tauxVente
) {
}
