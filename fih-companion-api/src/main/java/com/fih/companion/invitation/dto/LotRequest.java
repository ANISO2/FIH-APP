package com.fih.companion.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record LotRequest(
        @NotBlank(message = "Le numéro de série de début est obligatoire.")
        String startSerie,
        @NotBlank(message = "Le numéro de série de fin est obligatoire.")
        String endSerie,
        @NotBlank(message = "Le nom de base est obligatoire.")
        @Size(max = 200, message = "Le nom de base est trop long.")
        String baseName
) {
}
