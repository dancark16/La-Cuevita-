package com.tienda.products.variant;

import com.tienda.products.variant.dto.ImageResponse;
import com.tienda.products.variant.dto.VariantResponse;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class VariantMapper {

    public VariantResponse toResponse(ProductVariant variant) {
        List<ImageResponse> images = variant.getImages().stream()
                .map(img -> new ImageResponse(img.getId(), img.getUrl(), img.getSortOrder()))
                .toList();

        return new VariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                variant.getSize().getId(),
                variant.getSize().getLabel(),
                variant.getColor().getId(),
                variant.getColor().getName(),
                variant.getColor().getHex(),
                variant.getSku(),
                variant.getBarcode(),
                variant.getPrice(),
                variant.getPromoPrice(),
                variant.getStatus(),
                images,
                variant.getCreatedAt(),
                variant.getUpdatedAt());
    }
}
