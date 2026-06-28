package com.fih.companion.verification;

import java.time.LocalDate;
import java.util.List;


public record VerificationResult(
        String type,            // "BILLET" | "VOUCHER"
        Verdict verdict,
        String numeroserie,
        String codebarre,
        String eventTitle,
        LocalDate eventDate,
        String ticketModel,
        List<String> accessZones,
        int maxAccess,          // 0 = unlimited
        int usesSoFar,
        String holderName,      // billet only; from legacy holder table
        String affecteeA,       // from app-owned badge_affectation (null if none)
        Flags flags
) {
    public record Flags(
            boolean activation,
            boolean utilisation,
            boolean vendu,
            boolean reservation,
            boolean cancelled
    ) {
    }

     static VerificationResult notFound(String type, String code) {
        return new VerificationResult(
                type, Verdict.NOT_FOUND, null, code, null, null, null,
                List.of(), 0, 0, null, null,
                new Flags(false, false, false, false, false));
    }
}
