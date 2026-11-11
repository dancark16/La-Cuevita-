package com.tienda.inventory.dto;

import com.tienda.inventory.MovementType;

import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
        UUID id,
        UUID variantId,
        String sku,
        MovementType type,
        int quantity,
        String reason,
        String reference,
        UUID orderId,
        UUID userId,
        String userEmail,
        Instant createdAt
) {
}