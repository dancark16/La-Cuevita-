package com.tienda.catalogadmin;

import com.tienda.catalogadmin.dto.CatalogItemResponse;
import com.tienda.catalogadmin.dto.CatalogItemUpdate;
import com.tienda.catalogadmin.dto.ProductRowResponse;
import com.tienda.categories.Category;
import com.tienda.categories.CategoryRepository;
import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.inventory.Inventory;
import com.tienda.inventory.InventoryRepository;
import com.tienda.products.Product;
import com.tienda.products.ProductRepository;
import com.tienda.products.variant.ProductVariant;
import com.tienda.products.variant.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogAdminService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public Page<CatalogItemResponse> search(UUID categoryId, String q, String status, Pageable pageable) {
        String term = (q == null || q.isBlank()) ? null : q.trim();
        Page<ProductVariant> page = variantRepository.searchAdmin(categoryId, term, (status == null || status.isBlank()) ? null : status, pageable);

        List<UUID> ids = page.getContent().stream().map(ProductVariant::getId).toList();
        Map<UUID, Integer> stock = inventoryRepository.findByVariantIdIn(ids).stream()
                .collect(Collectors.toMap(i -> i.getVariant().getId(), Inventory::getQuantityOnHand));

        return page.map(variant -> toResponse(variant, stock.getOrDefault(variant.getId(), 0)));
    }

    /** Catálogo agrupado: un renglón por producto, con sus colores y tallas dentro. */
    @Transactional(readOnly = true)
    public Page<ProductRowResponse> searchProducts(UUID categoryId, String q, String status, Pageable pageable) {
        String term = (q == null || q.isBlank()) ? null : q.trim();
        Page<Product> page = productRepository.searchAdmin(categoryId, term,
                (status == null || status.isBlank()) ? null : status, pageable);

        List<UUID> productIds = page.getContent().stream().map(Product::getId).toList();
        List<ProductVariant> all = productIds.isEmpty()
                ? List.of()
                : variantRepository.findAvailableWithDetailsByProductIds(productIds);
        Map<UUID, Integer> stock = all.isEmpty() ? Map.of()
                : inventoryRepository.findByVariantIdIn(all.stream().map(ProductVariant::getId).toList()).stream()
                        .collect(Collectors.toMap(i -> i.getVariant().getId(), Inventory::getQuantityOnHand));
        Map<UUID, List<ProductVariant>> byProduct = all.stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        return page.map(product -> {
            List<ProductVariant> variants = byProduct.getOrDefault(product.getId(), List.of()).stream()
                    .sorted(Comparator.comparing((ProductVariant v) -> v.getColor().getName())
                            .thenComparing(v -> v.getSize().getLabel()))
                    .toList();
            List<ProductRowResponse.Variant> rows = variants.stream()
                    .map(v -> new ProductRowResponse.Variant(v.getId(), v.getSku(), v.getSize().getLabel(),
                            v.getColor().getName(), v.getColor().getHex(), v.getPrice(), v.getPromoPrice(),
                            stock.getOrDefault(v.getId(), 0)))
                    .toList();
            BigDecimal min = variants.stream().map(v -> v.getPrice()).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            BigDecimal max = variants.stream().map(v -> v.getPrice()).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            String thumb = variants.stream().flatMap(v -> v.getImages().stream())
                    .map(i -> i.getUrl()).findFirst().orElse(null);
            return new ProductRowResponse(product.getId(), product.getName(), product.getCategory().getId(),
                    product.getCategory().getName(), thumb, product.getBaseStatus(), min, max,
                    variants.stream().anyMatch(v -> v.getPromoPrice() != null),
                    rows.stream().mapToInt(ProductRowResponse.Variant::stock).sum(), rows);
        });
    }

    @Transactional
    public CatalogItemResponse update(UUID variantId, CatalogItemUpdate request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada: " + variantId));
        Product product = variant.getProduct();

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("El nombre no puede estar vacío");
            }
            product.setName(name);
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + request.categoryId()));
            product.setCategory(category);
        }
        if (request.price() != null) {
            variant.setPrice(request.price().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.promoPrice() != null) {
            BigDecimal promo = request.promoPrice().setScale(2, RoundingMode.HALF_UP);
            variant.setPromoPrice(promo.signum() == 0 ? null : promo);
        }
        if (variant.getPromoPrice() != null && variant.getPromoPrice().compareTo(variant.getPrice()) >= 0) {
            throw new IllegalArgumentException("El precio promocional debe ser menor al precio normal");
        }

        productRepository.save(product);
        variantRepository.save(variant);

        int stock = inventoryRepository.findByVariantId(variantId).map(Inventory::getQuantityOnHand).orElse(0);
        return toResponse(variant, stock);
    }

    private CatalogItemResponse toResponse(ProductVariant variant, int stock) {
        Product product = variant.getProduct();
        return new CatalogItemResponse(
                variant.getId(),
                variant.getImages().isEmpty() ? null : variant.getImages().get(0).getUrl(),
                product.getId(),
                variant.getSku(),
                product.getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                variant.getSize().getLabel(),
                variant.getColor().getName(),
                variant.getColor().getHex(),
                variant.getPrice(),
                variant.getPromoPrice(),
                stock,
                variant.getStatus(),
                product.getBaseStatus());
    }
}
