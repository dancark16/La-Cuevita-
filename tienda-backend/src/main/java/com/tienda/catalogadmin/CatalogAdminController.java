package com.tienda.catalogadmin;

import com.tienda.catalogadmin.dto.CatalogItemResponse;
import com.tienda.catalogadmin.dto.CatalogItemUpdate;
import com.tienda.catalogadmin.dto.ProductRowResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/catalog/items")
@RequiredArgsConstructor
public class CatalogAdminController {

    private final CatalogAdminService service;

    @GetMapping
    public Page<CatalogItemResponse> search(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = {"product.name", "sku"}) Pageable pageable) {
        return service.search(categoryId, q, status, pageable);
    }

    @GetMapping("/products")
    public Page<ProductRowResponse> searchProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.searchProducts(categoryId, q, status, pageable);
    }

    @PatchMapping("/{variantId}")
    public CatalogItemResponse update(
            @PathVariable UUID variantId,
            @Valid @RequestBody CatalogItemUpdate request) {
        return service.update(variantId, request);
    }
}
