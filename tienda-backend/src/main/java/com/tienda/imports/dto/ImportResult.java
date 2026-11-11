package com.tienda.imports.dto;

import java.math.BigDecimal;
import java.util.List;

public record ImportResult(
        boolean dryRun,
        String fileName,
        int totalRows,
        int created,
        int updated,
        int skipped,
        List<CategorySummary> categories,
        List<ImportItem> items,
        List<RowIssue> issues) {
    public enum Action {
        CREATE, UPDATE
    }

    public enum Level {
        ERROR, WARNING
    }

    public record CategorySummary(String name, int count, boolean isNew) {
    }

    public record ImportItem(int row, String sku, String name, String category, String size,
            BigDecimal price, BigDecimal cost, int stock, int currentStock, Action action) {
    }

    public record RowIssue(int row, Level level, String message) {
    }
}
