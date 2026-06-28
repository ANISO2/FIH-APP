package com.fih.companion.evenement;

import java.time.LocalDate;


public record EventDto(
        Integer reference,
        String titre,
        LocalDate date,
        boolean sellsTickets,
        boolean sellsVouchers,
        Integer locationId
) {
}
