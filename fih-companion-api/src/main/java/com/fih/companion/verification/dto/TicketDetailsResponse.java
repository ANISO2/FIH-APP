package com.fih.companion.verification.dto;

import java.time.LocalDate;
import java.util.List;


public record TicketDetailsResponse(
        String type,                 // "BILLET" | "VOUCHER"
        String numeroserie,
        String codebarre,
        Boolean livre,               // billet only
        LocalDate dateLivraison,     // billet only
        LocalDate dateVente,         // both (source differs)
        Integer commande,            // voucher only (voucherorder.code)
        List<AccessLogEntry> accessPublic,
        List<AccessLogEntry> accessVip
) {
}
