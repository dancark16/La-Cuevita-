package com.tienda.products.dto;

import com.tienda.products.ProductStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String name,
        String description,
        ProductStatus baseStatus,
        Instant createdAt,
        Instant updatedAt
        ) {

}
