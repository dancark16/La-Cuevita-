package com.tienda.products.variant.dto;

import jakarta.validation.constraints.NotBlank;

public record SizeRequest(@NotBlank String label, Boolean active) {
}