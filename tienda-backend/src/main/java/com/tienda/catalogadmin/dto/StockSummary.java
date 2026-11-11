package com.tienda.catalogadmin.dto;

import java.math.BigDecimal;

/** Resumen del inventario activo. Los valores se calculan por variante: cantidad x precio (o costo). */
public record StockSummary(
        Long units,
        BigDecimal retailValue,
        BigDecimal costValue,
        Long variants,
        Long outOfStock,
        Long lowStock
) {
    public StockSummary {
        units = units == null ? 0L : units;
        retailValue = retailValue == null ? BigDecimal.ZERO : retailValue;
        costValue = costValue == null ? BigDecimal.ZERO : costValue;
        variants = variants == null ? 0L : variants;
        outOfStock = outOfStock == null ? 0L : outOfStock;
        lowStock = lowStock == null ? 0L : lowStock;
    }
}
