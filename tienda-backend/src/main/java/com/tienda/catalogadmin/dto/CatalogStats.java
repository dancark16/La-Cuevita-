package com.tienda.catalogadmin.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CatalogStats(Map<String, Long> productsByStatus, List<CategoryCount> categories) {

    public record CategoryCount(UUID id, String name, long products) {
    }
}
