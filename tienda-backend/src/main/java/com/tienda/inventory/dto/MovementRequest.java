package com.tienda.inventory.dto;

import com.tienda.inventory.MovementType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MovementRequest(
        @NotNull UUID variantId,
        @NotNull MovementType type,
        @NotNull Integer quantity,
        String reason,
        String reference,
        UUID orderId
) {
}
