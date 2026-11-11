package com.tienda.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreCatalogController {

    private static final int MAX_PAGE_SIZE = 48;

    private final StoreCatalogService service;

    @GetMapping("/products")
    public Page<StoreDto.ProductSummary> search(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sizeLabel,
            @RequestParam(required = false) String colorName,
            @RequestParam(defaultValue = "NEWEST") StoreDto.Sort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Sort order = sort == StoreDto.Sort.NAME
                ? Sort.by(Sort.Direction.ASC, "name")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return service.search(categoryId, q, sizeLabel, colorName, PageRequest.of(safePage, safeSize, order));
    }

    @GetMapping("/facets")
    public StoreDto.Facets facets() {
        return service.facets();
    }

    @GetMapping("/products/{id}")
    public StoreDto.ProductDetail detail(@PathVariable UUID id) {
        return service.detail(id);
    }
}
