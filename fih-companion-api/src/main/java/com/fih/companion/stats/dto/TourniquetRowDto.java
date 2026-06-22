package com.fih.companion.stats.dto;

/**
 * One model row of "Statistique des tourniquets". Audience is the per-row total
 * of accessible codes (billet + voucher).
 *
 * Code à barre accessibles : billetCodes | voucherCodes | audience
 * Transactions tourniquet  : billetTransactions | voucherTransactions
 */
public record TourniquetRowDto(
        int modelId,
        String modelName,
        long billetCodes,
        long voucherCodes,
        long audience,
        long billetTransactions,
        long voucherTransactions
) {
}
