package com.tienda.inventory;

import com.tienda.catalogadmin.dto.StockSummary;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByVariantId(UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variant.id = :variantId")
    Optional<Inventory> findByVariantIdForUpdate(UUID variantId);

    List<Inventory> findByVariantIdIn(Collection<UUID> variantIds);

    @Query("""
            SELECT new com.tienda.catalogadmin.dto.StockSummary(
                SUM(i.quantityOnHand),
                SUM(i.quantityOnHand * v.price),
                SUM(i.quantityOnHand * v.cost),
                COUNT(i),
                SUM(CASE WHEN i.quantityOnHand = 0 THEN 1L ELSE 0L END),
                SUM(CASE WHEN i.quantityOnHand BETWEEN 1 AND 2 THEN 1L ELSE 0L END))
            FROM Inventory i JOIN i.variant v
            WHERE v.status = com.tienda.products.variant.VariantStatus.ACTIVE
            """)
    StockSummary summarizeActiveStock();
}
