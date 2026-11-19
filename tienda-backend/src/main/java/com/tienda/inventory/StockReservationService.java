package com.tienda.inventory;

import com.tienda.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reserva de stock para pedidos pendientes de pago.
 *
 * Disponible = en mano - reservado. Al crear el pedido se reserva; al confirmar el pago la
 * reserva se convierte en venta (baja el stock en mano); si el pedido se cancela, vence o se
 * borra, la reserva se libera.
 */
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public void reserve(UUID variantId, String sku, int quantity) {
        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado para la variante: " + variantId));
        int available = inventory.getQuantityOnHand() - inventory.getQuantityReserved();
        if (available < quantity) {
            throw new InsufficientStockException(
                    "Stock insuficiente para el SKU %s: disponible %d, solicitado %d"
                            .formatted(sku, Math.max(available, 0), quantity));
        }
        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        inventoryRepository.save(inventory);
    }

    /** Libera unidades reservadas (nunca deja el reservado en negativo). */
    @Transactional
    public void release(UUID variantId, int quantity) {
        inventoryRepository.findByVariantIdForUpdate(variantId).ifPresent(inventory -> {
            inventory.setQuantityReserved(Math.max(0, inventory.getQuantityReserved() - quantity));
            inventoryRepository.save(inventory);
        });
    }
}
