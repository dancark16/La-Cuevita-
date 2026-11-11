package com.tienda.products.variant;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.products.Product;
import com.tienda.products.ProductRepository;
import com.tienda.products.variant.dto.ImageRequest;
import com.tienda.products.variant.dto.VariantRequest;
import com.tienda.products.variant.dto.VariantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tienda.inventory.InventoryService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final VariantMapper variantMapper;
    private final InventoryService inventoryService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<VariantResponse> findByProduct(UUID productId) {
        return variantRepository.findByProductId(productId).stream()
                .filter(v -> v.getStatus() != VariantStatus.DISCONTINUED)
                .map(variantMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse findById(UUID id) {
        return variantMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public VariantResponse create(VariantRequest request) {
        if (variantRepository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("Ya existe una variante con el SKU: " + request.sku());
        }

        ProductVariant variant = ProductVariant.builder()
                .product(getProductOrThrow(request.productId()))
                .size(getSizeOrThrow(request.sizeId()))
                .color(getColorOrThrow(request.colorId()))
                .sku(request.sku())
                .barcode(request.barcode())
                .price(request.price())
                .promoPrice(request.promoPrice())
                .status(request.status())
                .build();

        ProductVariant saved = variantRepository.save(variant);
        inventoryService.initializeForVariant(saved);

        return variantMapper.toResponse(saved);
    }

    @Transactional
    public VariantResponse update(UUID id, VariantRequest request) {
        ProductVariant variant = getOrThrow(id);

        if (variantRepository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new IllegalArgumentException("Ya existe otra variante con el SKU: " + request.sku());
        }

        variant.setProduct(getProductOrThrow(request.productId()));
        variant.setSize(getSizeOrThrow(request.sizeId()));
        variant.setColor(getColorOrThrow(request.colorId()));
        variant.setSku(request.sku());
        variant.setBarcode(request.barcode());
        variant.setPrice(request.price());
        variant.setPromoPrice(request.promoPrice());
        if (request.status() != null) {
            variant.setStatus(request.status());
        }

        return variantMapper.toResponse(variantRepository.save(variant));
    }

    @Transactional
    public void discontinue(UUID id) {
        ProductVariant variant = getOrThrow(id);
        long sold = entityManager
                .createQuery("SELECT COUNT(i) FROM OrderItem i WHERE i.variant.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        if (sold > 0) {
            // Ya tiene pedidos: se conserva para no romper el historial, pero deja de verse.
            variant.setStatus(VariantStatus.DISCONTINUED);
            variantRepository.save(variant);
            return;
        }
        // Nunca se vendió: se elimina por completo, con su inventario y movimientos.
        entityManager.createQuery(
                "DELETE FROM InventoryMovement m WHERE m.inventory.id IN (SELECT n.id FROM Inventory n WHERE n.variant.id = :id)")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM Inventory n WHERE n.variant.id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        variantRepository.deleteById(id);
    }

    @Transactional
    public VariantResponse addImage(UUID variantId, ImageRequest request) {
        ProductVariant variant = getOrThrow(variantId);
        ProductImage image = ProductImage.builder()
                .variant(variant)
                .url(request.url())
                .sortOrder(request.sortOrder())
                .build();
        variant.getImages().add(image);
        return variantMapper.toResponse(variantRepository.save(variant));
    }

    @Transactional
    public VariantResponse removeImage(UUID variantId, UUID imageId) {
        ProductVariant variant = getOrThrow(variantId);
        boolean removed = variant.getImages().removeIf(img -> img.getId().equals(imageId));
        if (!removed) {
            throw new ResourceNotFoundException("Imagen no encontrada en esta variante: " + imageId);
        }
        return variantMapper.toResponse(variantRepository.save(variant));
    }

    private Product getProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private Size getSizeOrThrow(UUID id) {
        return sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Talla no encontrada: " + id));
    }

    private Color getColorOrThrow(UUID id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Color no encontrado: " + id));
    }

    private ProductVariant getOrThrow(UUID id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada: " + id));
    }
}
