package com.fih.companion.stats.dto;

import java.time.LocalDate;


public record RecetteSummaryDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        double billet,   // revenue TND from billets  (sum of counterbillet  * prix)
        double voucher,  // revenue TND from vouchers (sum of countervoucher * prix)
        double total     // billet + voucher
) {
}
