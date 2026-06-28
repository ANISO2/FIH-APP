package com.fih.companion.stats.dto;


public record RecetteModelRowDto(
        int modelId,
        String modelName,
        double montant,
        long billetGeneration,
        long billetVente,
        long billetReste,
        long voucherGeneration,
        long voucherVente,
        long voucherReste,
        long totalVendu,
        double recetteTnd,
        double tauxVente
) {
}
