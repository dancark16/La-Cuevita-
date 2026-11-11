package com.tienda.products.variant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ImageRequest(
        @NotBlank
        String url,
        @NotNull
        @PositiveOrZero
        Integer sortOrder
        ) {

}
