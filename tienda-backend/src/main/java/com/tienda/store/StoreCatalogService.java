package com.tienda.store;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.inventory.Inventory;
import com.tienda.inventory.InventoryRepository;
import com.tienda.products.Product;
import com.tienda.products.ProductRepository;
import com.tienda.products.ProductStatus;
import com.tienda.products.variant.ProductImage;
import com.tienda.products.variant.ProductVariant;
import com.tienda.products.variant.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreCatalogService {

    private static final int MAX_QUANTITY_PER_ORDER = 10;
    private static final List<String> LETTER_SIZES = List.of("XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;

    @Value("${app.store.new-product-days}")
    private int newProductDays;

    @Transactional(readOnly = true)
    public Page<StoreDto.ProductSummary> search(UUID categoryId, String q, String size, String color, Pageable pageable) {
        String normalizedQuery = (q == null || q.isBlank()) ? null : q.trim();
        Page<Product> page = productRepository.searchStorefront(categoryId, normalizedQuery, blankToNull(size), blankToNull(color), pageable);

        List<UUID> productIds = page.getContent().stream().map(Product::getId).toList();
        Map<UUID, List<ProductVariant>> variantsByProduct = loadVariants(productIds);
        Map<UUID, Integer> stock = loadStock(variantsByProduct.values().stream().flatMap(List::stream).toList());

        return page
                .map(product -> toSummary(product, variantsByProduct.getOrDefault(product.getId(), List.of()), stock));
    }

    @Transactional(readOnly = true)
    public StoreDto.Facets facets() {
        List<String> sizes = variantRepository.findActiveSizeLabels();
        List<StoreDto.ColorSwatch> colors = variantRepository.findActiveColors().stream()
                .map(row -> new StoreDto.ColorSwatch((String) row[0], (String) row[1]))
                .toList();
        return new StoreDto.Facets(sizes, colors);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public StoreDto.ProductDetail detail(UUID productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getBaseStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));

        List<ProductVariant> variants = loadVariants(List.of(productId)).getOrDefault(productId, List.of());
        Map<UUID, Integer> stock = loadStock(variants);

        List<StoreDto.Variant> result = variants.stream()
                .sorted(Comparator.comparing((ProductVariant v) -> v.getColor().getName())
                        .thenComparingInt(v -> sizeRank(v.getSize().getLabel()))
                        .thenComparing(v -> v.getSize().getLabel()))
                .map(variant -> toVariant(variant, stock.getOrDefault(variant.getId(), 0)))
                .toList();

        return new StoreDto.ProductDetail(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                result);
    }

    private Map<UUID, List<ProductVariant>> loadVariants(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return variantRepository.findActiveWithDetailsByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(variant -> variant.getProduct().getId()));
    }

    private Map<UUID, Integer> loadStock(Collection<ProductVariant> variants) {
        if (variants.isEmpty()) {
            return Map.of();
        }
        List<UUID> variantIds = variants.stream().map(ProductVariant::getId).toList();
        return inventoryRepository.findByVariantIdIn(variantIds).stream()
                .collect(Collectors.toMap(
                        inventory -> inventory.getVariant().getId(),
                        Inventory::getQuantityOnHand));
    }

    private StoreDto.ProductSummary toSummary(Product product, List<ProductVariant> variants,
            Map<UUID, Integer> stock) {
        ProductVariant cheapest = variants.stream()
                .min(Comparator.comparing(this::effectivePrice))
                .orElse(null);

        BigDecimal price = cheapest != null ? effectivePrice(cheapest) : BigDecimal.ZERO;
        BigDecimal compareAt = cheapest != null ? compareAtPrice(cheapest) : null;

        String imageUrl = variants.stream()
                .flatMap(variant -> variant.getImages().stream())
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);

        Map<String, StoreDto.ColorSwatch> colors = new LinkedHashMap<>();
        for (ProductVariant variant : variants) {
            colors.putIfAbsent(variant.getColor().getName(),
                    new StoreDto.ColorSwatch(variant.getColor().getName(), variant.getColor().getHex()));
        }

        List<StoreDto.VariantOption> options = variants.stream()
                .sorted(Comparator.comparingInt((ProductVariant v) -> sizeRank(v.getSize().getLabel()))
                        .thenComparing(v -> v.getColor().getName()))
                .map(variant -> new StoreDto.VariantOption(
                        variant.getId(),
                        variant.getSize().getLabel(),
                        variant.getColor().getName(),
                        variant.getColor().getHex(),
                        effectivePrice(variant),
                        cappedQuantity(stock.getOrDefault(variant.getId(), 0))))
                .toList();

        boolean inStock = options.stream().anyMatch(option -> option.maxQuantity() > 0);
        boolean newArrival = product.getCreatedAt().isAfter(Instant.now().minus(newProductDays, ChronoUnit.DAYS));

        return new StoreDto.ProductSummary(
                product.getId(),
                product.getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                price,
                compareAt,
                imageUrl,
                List.copyOf(colors.values()),
                inStock,
                newArrival,
                options);
    }

    private StoreDto.Variant toVariant(ProductVariant variant, int onHand) {
        return new StoreDto.Variant(
                variant.getId(),
                variant.getSku(),
                variant.getSize().getLabel(),
                variant.getColor().getName(),
                variant.getColor().getHex(),
                effectivePrice(variant),
                compareAtPrice(variant),
                variant.getImages().stream().map(ProductImage::getUrl).toList(),
                cappedQuantity(onHand));
    }

    private int cappedQuantity(int onHand) {
        return Math.min(Math.max(onHand, 0), MAX_QUANTITY_PER_ORDER);
    }

    private static int sizeRank(String label) {
        String normalized = label.trim().toUpperCase();
        int letterIndex = LETTER_SIZES.indexOf(normalized);
        if (letterIndex >= 0) {
            return letterIndex;
        }
        try {
            return 100 + Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return 1000;
        }
    }

    private BigDecimal effectivePrice(ProductVariant variant) {
        return variant.getPromoPrice() != null ? variant.getPromoPrice() : variant.getPrice();
    }

    private BigDecimal compareAtPrice(ProductVariant variant) {
        BigDecimal promo = variant.getPromoPrice();
        return promo != null && promo.compareTo(variant.getPrice()) < 0 ? variant.getPrice() : null;
    }
}