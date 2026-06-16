package com.fih.companion.verification;

import java.time.LocalDate;
import java.util.List;

/**
 * Uniform response for both billet and voucher checks, so the mobile UI is
 * identical. All fields are computed read-only; nothing is mutated.
 */
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
        String holderName,      // billet only; null otherwise
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

    /** Helper for the NOT_FOUND case where we only know the code searched. */
    static VerificationResult notFound(String type, String code) {
        return new VerificationResult(
                type, Verdict.NOT_FOUND, null, code, null, null, null,
                List.of(), 0, 0, null,
                new Flags(false, false, false, false, false));
    }
}
