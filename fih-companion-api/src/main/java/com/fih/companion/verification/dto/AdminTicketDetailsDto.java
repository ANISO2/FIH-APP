package com.fih.companion.verification.dto;

import java.util.List;


public record AdminTicketDetailsDto(
        String type,            // "BILLET" | "VOUCHER"
        String numeroserie,
        String codebarre,
        String eventTitle,      // Spectacle
        String ticketModel,     // Modèle de billet
        boolean vente,          // flags.vendu
        boolean utilisation,
        boolean reservation,
        boolean activation,
        List<AccessLogEntry> publicLog,
        List<AccessLogEntry> vipLog
) {
}
