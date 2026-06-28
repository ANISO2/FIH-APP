package com.fih.companion.stats.dto;


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
