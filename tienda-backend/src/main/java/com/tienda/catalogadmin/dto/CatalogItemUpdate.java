package com.tienda.catalogadmin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Edición parcial: los campos nulos no se modifican.
 * name y categoryId pertenecen al producto (afectan a todas sus variantes);
 * price y promoPrice pertenecen a la variante. promoPrice = 0 quita la promoción.
 */
public record CatalogItemUpdate(
        @Size(min = 1, max = 150) String name,
        UUID categoryId,
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0") BigDecimal price,
        @DecimalMin(value = "0", message = "El precio promocional no puede ser negativo") BigDecimal promoPrice
) {
}
