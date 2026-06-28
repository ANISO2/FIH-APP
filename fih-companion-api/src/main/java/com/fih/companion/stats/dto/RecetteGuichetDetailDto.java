package com.fih.companion.stats.dto;

import java.time.LocalDate;


public record RecetteGuichetDetailDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        int modelId,
        String modelName,
        long billetLivraison,
        long billetVente,
        double billetPrixUnitaire,
        double billetRecette,
        long billetReste,
        double kit
) {
}
