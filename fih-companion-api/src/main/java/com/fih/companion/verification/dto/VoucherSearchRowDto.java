package com.fih.companion.verification.dto;

/** One row of the backoffice "Vérification Voucher" list (3.2). */
public record VoucherSearchRowDto(
        String eventTitle,
        String modelName,
        String numeroserie,
        String codebarre,
        boolean utilisation,
        boolean vendu,
        boolean activation,
        boolean reservation,
        String commande
) {
}
