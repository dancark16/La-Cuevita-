package com.tienda.products.dto;

import com.tienda.products.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProductRequest(
        @NotNull
        UUID categoryId,
        @NotBlank
        String name,
        String description,
        ProductStatus baseStatus
        ) {

}
