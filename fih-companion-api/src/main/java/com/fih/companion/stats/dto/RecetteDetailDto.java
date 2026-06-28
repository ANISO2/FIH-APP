package com.fih.companion.stats.dto;

import java.time.LocalDate;


public record RecetteDetailDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        int modelId,
        String modelName,
        double montant,
        long voucherGeneration,
        long voucherVente,
        long voucherReste,
        long billetGeneration,
        long billetVente,
        long billetReste,
        long kitGeneration,
        long kitVente,
        long kitReste,
        long total,
        double recetteTnd
) {
}
