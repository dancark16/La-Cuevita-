package com.tienda.products;

import com.tienda.products.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getDescription(),
                product.getBaseStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
