package com.tienda.catalogadmin;

import com.tienda.catalogadmin.dto.CatalogStats;
import com.tienda.catalogadmin.dto.StockSummary;
import com.tienda.inventory.InventoryRepository;
import com.tienda.products.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @GetMapping("/stock")
    @Transactional(readOnly = true)
    public StockSummary stock() {
        return inventoryRepository.summarizeActiveStock();
    }

    @GetMapping("/catalog")
    @Transactional(readOnly = true)
    public CatalogStats catalog() {
        Map<String, Long> byStatus = new HashMap<>(Map.of("ACTIVE", 0L, "DRAFT", 0L, "INACTIVE", 0L));
        for (Object[] row : productRepository.countByStatus()) {
            byStatus.put(String.valueOf(row[0]), (Long) row[1]);
        }
        List<CatalogStats.CategoryCount> categories = productRepository.countByCategory().stream()
                .map(row -> new CatalogStats.CategoryCount((UUID) row[0], (String) row[1], (Long) row[2]))
                .sorted((a, b) -> Long.compare(b.products(), a.products()))
                .toList();
        return new CatalogStats(byStatus, categories);
    }
}
