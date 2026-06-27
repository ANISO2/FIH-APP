package com.fih.companion.verification.dto;

import java.util.List;

/**
 * Details payload for the backoffice verification modal (3.2), matching the
 * details mockups: the four header flags + identity, then the Public and VIP
 * access logs. Assembled read-only from the live verify lookup (flags/identity)
 * and the indexed access-log queries; nothing here is cached.
 */
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
