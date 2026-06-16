package com.fih.companion.evenement;

import java.time.LocalDate;

/**
 * What we expose over HTTP. We never serialize the JPA entity directly:
 * the DTO is a stable, intentional API shape decoupled from the database.
 */
public record EventDto(
        Integer reference,
        String titre,
        LocalDate date,
        boolean sellsTickets,
        boolean sellsVouchers,
        Integer locationId
) {
}
