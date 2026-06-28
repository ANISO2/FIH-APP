package com.fih.companion.stats.dto;

import java.time.LocalDate;

public record RecetteEventHeaderDto(
        int eventId,
        String eventTitle,
        LocalDate eventDate,
        long totalGenere,
        long totalVendu,
        long totalReste,
        double recetteTotale,
        double tauxVente
) {
}
