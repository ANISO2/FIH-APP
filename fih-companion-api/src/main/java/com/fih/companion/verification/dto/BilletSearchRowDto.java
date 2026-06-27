package com.fih.companion.verification.dto;

import java.time.LocalDate;

/** One row of the backoffice "Vérification Billet" list (3.2). */
public record BilletSearchRowDto(
        String numeroserie,
        String codebarre,
        boolean activation,
        boolean livre,
        boolean vendu,
        boolean utilise,
        String eventTitle,
        String modelName,
        LocalDate dateVente,
        String livreur,
        LocalDate dateLivraison
) {
}
