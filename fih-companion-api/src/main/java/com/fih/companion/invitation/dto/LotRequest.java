package com.fih.companion.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of the lot (batch-by-range) endpoints.
 *
 * startSerie / endSerie are INVITATION serials (numeroserie). In this database
 * the numeroserie is a fixed-width 10-digit numeric string (e.g. 8250000000),
 * so a simple range [start, end] selects a contiguous block. baseName is the
 * common name; each record gets baseName-01, baseName-02, ...
 */
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
