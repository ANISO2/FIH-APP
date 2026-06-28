package com.fih.companion.verification.dto;

import java.time.LocalDate;


public record ExternalVoucherInfo(
        String status,
        String code,
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
}
