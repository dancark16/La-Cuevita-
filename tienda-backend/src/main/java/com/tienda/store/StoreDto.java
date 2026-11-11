package com.tienda.store;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class StoreDto {

        private StoreDto() {
        }

        public enum Sort {
                NEWEST, NAME
        }

        public record ColorSwatch(String name, String hex) {

        }

        public record Facets(List<String> sizes, List<ColorSwatch> colors) {

        }

        public record VariantOption(
                        UUID id,
                        String sizeLabel,
                        String colorName,
                        String colorHex,
                        BigDecimal price,
                        int maxQuantity) {

        }

        public record ProductSummary(
                        UUID id,
                        String name,
                        UUID categoryId,
                        String categoryName,
                        BigDecimal price,
                        BigDecimal compareAtPrice,
                        String imageUrl,
                        List<ColorSwatch> colors,
                        boolean inStock,
                        boolean newArrival,
                        List<VariantOption> variants) {

        }

        public record Variant(
                        UUID id,
                        String sku,
                        String sizeLabel,
                        String colorName,
                        String colorHex,
                        BigDecimal price,
                        BigDecimal compareAtPrice,
                        List<String> images,
                        int maxQuantity) {

        }

        public record ProductDetail(
                        UUID id,
                        String name,
                        String description,
                        UUID categoryId,
                        String categoryName,
                        List<Variant> variants) {

        }
}