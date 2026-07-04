package com.fih.companion.verification.dto;

public record VoucherSearchRowDto(
        String eventTitle,
        String modelName,
        String numeroserie,
        String codebarre,
        boolean utilisation,
        boolean vendu,
        boolean activation,
        boolean reservation,
        Integer commande
) {
}
