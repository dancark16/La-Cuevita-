package com.tienda.categories.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
                UUID id,
                String name,
                String slug,
                UUID parentId,
                String imageUrl,
                boolean active,
                Instant createdAt) {

}