package com.tienda.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID variantId,
        String sku,
        int quantityOnHand,
        int quantityReserved,
        Instant updatedAt
        ) {

}
