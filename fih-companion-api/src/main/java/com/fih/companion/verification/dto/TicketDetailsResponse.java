package com.fih.companion.verification.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Lazy-loaded extras for the ℹ details screen. The hot verify path stays lean;
 * this is only fetched when the operator opens the details. One uniform shape
 * for billet and voucher — billet fills livre/dateLivraison/dateVente, voucher
 * fills commande/dateVente; unused fields are null.
 *
 * Access history comes from tturnstile (Public) and vipaccess (VIP), queried by
 * the INDEXED numeroserie (billet/voucher FK), newest first, capped.
 */
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
