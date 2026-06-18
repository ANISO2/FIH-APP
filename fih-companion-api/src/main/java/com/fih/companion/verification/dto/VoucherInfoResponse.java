package com.fih.companion.verification.dto;

import java.time.LocalDate;

/**
 * Full information about a paid voucher (Billet Gradins), as returned by the
 * EXTERNAL web service owned by the other team (Feature 1).
 *
 * This is the contract the Flutter app is built against NOW, before the external
 * service is wired in. The shape is fixed here; only the implementation behind
 * {@code VoucherVerificationGateway} changes later. Fields are nullable on
 * purpose so a "pending integration" or "not found" response can omit them
 * without breaking the client.
 *
 * status values:
 *   - "OK"                   the external service found and described the voucher
 *   - "NOT_FOUND"            the external service has no such voucher
 *   - "PENDING_INTEGRATION"  the external service is not wired in yet (current stub)
 */
public record VoucherInfoResponse(
        String status,
        String source,          // always "EXTERNAL_SERVICE" — verification is not ours
        String code,            // the scanned code we were asked about
        String numeroserie,
        String codebarre,
        String eventTitle,
        LocalDate eventDate,
        String model,
        Double prix,
        Boolean vendu,
        LocalDate dateVente,
        Integer accessCounter,
        String message
) {
    /** The current placeholder answer: the external service is not connected yet. */
    public static VoucherInfoResponse pendingIntegration(String code) {
        return new VoucherInfoResponse(
                "PENDING_INTEGRATION", "EXTERNAL_SERVICE", code,
                null, null, null, null, null, null, null, null, null,
                "Vérification déléguée au service externe (équipe billetterie) — "
                        + "intégration à venir. Le contrat de réponse est déjà figé.");
    }
}
