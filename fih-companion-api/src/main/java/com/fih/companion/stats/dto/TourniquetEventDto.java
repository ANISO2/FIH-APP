package com.fih.companion.stats.dto;

import java.time.LocalDate;
import java.util.List;


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
