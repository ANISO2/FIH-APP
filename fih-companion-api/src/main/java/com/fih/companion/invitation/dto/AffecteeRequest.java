package com.fih.companion.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

 public record AffecteeRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 255, message = "Le nom ne peut pas dépasser 255 caractères.")
        String name
) {
}
