package com.tienda.products.variant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ColorRequest(
        @NotBlank
        String name,
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un hex válido, ej: #1A1A1A")
        String hex,
        Boolean active
        ) {

}
