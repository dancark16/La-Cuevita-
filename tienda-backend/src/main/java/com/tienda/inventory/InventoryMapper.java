package com.tienda.inventory;

import com.tienda.inventory.dto.InventoryResponse;
import com.tienda.inventory.dto.MovementResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getVariant().getId(),
                inventory.getVariant().getSku(),
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getUpdatedAt());
    }

    public MovementResponse toResponse(InventoryMovement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getInventory().getVariant().getId(),
                movement.getInventory().getVariant().getSku(),
                movement.getType(),
                movement.getQuantity(),
                movement.getReason(),
                movement.getReference(),
                movement.getOrderId(),
                movement.getUser() != null ? movement.getUser().getId() : null,
                movement.getUser() != null ? movement.getUser().getEmail() : null,
                movement.getCreatedAt());
    }
}
