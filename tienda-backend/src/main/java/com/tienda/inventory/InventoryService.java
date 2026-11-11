package com.tienda.inventory;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.inventory.dto.InventoryResponse;
import com.tienda.inventory.dto.MovementRequest;
import com.tienda.inventory.dto.MovementResponse;
import com.tienda.products.variant.ProductVariant;
import com.tienda.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Set<MovementType> INCREASING = Set.of(
            MovementType.ENTRADA, MovementType.DEVOLUCION, MovementType.ANULACION);
    private static final Set<MovementType> DECREASING = Set.of(
            MovementType.SALIDA, MovementType.VENTA);

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public void initializeForVariant(ProductVariant variant) {
        if (inventoryRepository.findByVariantId(variant.getId()).isPresent()) {
            return;
        }
        Inventory inventory = Inventory.builder()
                .variant(variant)
                .quantityOnHand(0)
                .quantityReserved(0)
                .build();
        inventoryRepository.save(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByVariantId(UUID variantId) {
        return inventoryMapper.toResponse(getOrThrow(variantId));
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> listMovements(UUID variantId, Pageable pageable) {
        Inventory inventory = getOrThrow(variantId);
        return movementRepository.findByInventoryIdOrderByCreatedAtDesc(inventory.getId(), pageable)
                .map(inventoryMapper::toResponse);
    }

    @Transactional
    public MovementResponse registerMovement(MovementRequest request, User actingUser) {
        int delta = resolveDelta(request.type(), request.quantity());

        Inventory inventory = inventoryRepository.findByVariantIdForUpdate(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado para la variante: " + request.variantId()));

        int newQuantity = inventory.getQuantityOnHand() + delta;
        if (newQuantity < 0) {
            throw new InsufficientStockException(
                    "Stock insuficiente para el SKU %s: disponible %d, se intentó descontar %d"
                            .formatted(inventory.getVariant().getSku(), inventory.getQuantityOnHand(), Math.abs(delta)));
        }

        inventory.setQuantityOnHand(newQuantity);
        inventoryRepository.save(inventory);

        InventoryMovement movement = InventoryMovement.builder()
                .inventory(inventory)
                .type(request.type())
                .quantity(request.quantity())
                .reason(request.reason())
                .reference(request.reference())
                .orderId(request.orderId())
                .user(actingUser)
                .build();

        return inventoryMapper.toResponse(movementRepository.save(movement));
    }

    private int resolveDelta(MovementType type, int quantity) {
        if (type == MovementType.AJUSTE) {
            if (quantity == 0) {
                throw new IllegalArgumentException("La cantidad de un ajuste no puede ser cero");
            }
            return quantity;
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero para el tipo " + type);
        }

        if (INCREASING.contains(type)) {
            return quantity;
        }
        if (DECREASING.contains(type)) {
            return -quantity;
        }
        throw new IllegalArgumentException("Tipo de movimiento no soportado: " + type);
    }

    private Inventory getOrThrow(UUID variantId) {
        return inventoryRepository.findByVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado para la variante: " + variantId));
    }
}
