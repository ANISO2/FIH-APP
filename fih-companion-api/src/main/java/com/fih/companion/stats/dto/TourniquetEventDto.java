package com.fih.companion.stats.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * One spectacle (event) block of "Statistique des tourniquets": the per-event
 * header line of totals plus the per-model rows. The header totals are summed
 * from the rows in the service, so they always match what is displayed.
 */
public record TourniquetEventDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        long audience,              // sum of (billetCodes + voucherCodes)
        long transactionsBillets,   // sum of billet scans
        long transactionsVouchers,  // sum of voucher scans
        long tourniquets,           // transactionsBillets + transactionsVouchers
        List<TourniquetRowDto> rows
) {
}
