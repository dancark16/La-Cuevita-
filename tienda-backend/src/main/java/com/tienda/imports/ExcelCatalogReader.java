package com.tienda.imports;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
class ExcelCatalogReader {

    record RawRow(int rowNumber, String code, String name, String category, BigDecimal price, BigDecimal cost, BigDecimal stock) {
    }

    List<RawRow> read(InputStream input) {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();

            Map<String, Integer> columns = null;
            int headerIndex = -1;
            for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, Integer> found = headers(row, formatter, evaluator);
                if (found.containsKey("code") && found.containsKey("name") && found.containsKey("price")) {
                    columns = found;
                    headerIndex = i;
                    break;
                }
            }
            if (columns == null) {
                throw new IllegalArgumentException(
                        "No se encontraron los encabezados (Código, Producto, Pvp1). ¿Es el reporte de inventario de Codesoft?");
            }

            List<RawRow> rows = new ArrayList<>();
            for (int i = headerIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String code = text(row, columns.get("code"), formatter, evaluator);
                String name = text(row, columns.get("name"), formatter, evaluator);
                if (code.isBlank() && name.isBlank()) {
                    continue;
                }
                rows.add(new RawRow(
                        i + 1,
                        code,
                        name,
                        text(row, columns.get("category"), formatter, evaluator),
                        number(row, columns.get("price"), evaluator),
                        number(row, columns.get("cost"), evaluator),
                        number(row, columns.get("stock"), evaluator)));
            }
            return rows;
        } catch (IOException | EncryptedDocumentException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo. Debe ser un Excel (.xlsx o .xls) válido.");
        }
    }

    private Map<String, Integer> headers(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<String, Integer> found = new HashMap<>();
        for (Cell cell : row) {
            String key = headerKey(formatter.formatCellValue(cell, evaluator));
            switch (key) {
                case "codigo", "cdigo" -> found.putIfAbsent("code", cell.getColumnIndex());
                case "producto" -> found.putIfAbsent("name", cell.getColumnIndex());
                case "categoria" -> found.putIfAbsent("category", cell.getColumnIndex());
                case "pvp1" -> found.putIfAbsent("price", cell.getColumnIndex());
                case "costo" -> found.putIfAbsent("cost", cell.getColumnIndex());
                case "stock" -> found.putIfAbsent("stock", cell.getColumnIndex());
                default -> {
                }
            }
        }
        return found;
    }

    private String headerKey(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("\uFFFD", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private String text(Row row, Integer column, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (column == null) {
            return "";
        }
        Cell cell = row.getCell(column);
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
    }

    private BigDecimal number(Row row, Integer column, FormulaEvaluator evaluator) {
        if (column == null) {
            return null;
        }
        Cell cell = row.getCell(column);
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = evaluator.evaluateFormulaCell(cell);
        }
        try {
            if (type == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
            }
            if (type == CellType.STRING) {
                String value = cell.getStringCellValue().trim().replace(',', '.');
                return value.isEmpty() ? null : new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }
}