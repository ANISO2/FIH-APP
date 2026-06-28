package com.fih.companion.verification.dto;

import java.time.LocalDate;


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
     public static VoucherInfoResponse pendingIntegration(String code) {
        return new VoucherInfoResponse(
                "PENDING_INTEGRATION", "EXTERNAL_SERVICE", code,
                null, null, null, null, null, null, null, null, null,
                "Vérification déléguée au service externe (équipe billetterie) — "
                        + "intégration à venir. Le contrat de réponse est déjà figé.");
    }
}
