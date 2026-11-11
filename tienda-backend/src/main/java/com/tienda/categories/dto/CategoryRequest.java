package com.tienda.categories.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CategoryRequest(
                @NotBlank String name,
                @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "El slug debe ser minúsculas, números y guiones (ej: ropa-hombre)") String slug,
                UUID parentId,
                @Size(max = 500, message = "La URL de la imagen no puede superar 500 caracteres") @Pattern(regexp = "^$|^https?://\\S+$", message = "La imagen debe ser una URL que empiece con http:// o https://") String imageUrl,
                Boolean active) {

}