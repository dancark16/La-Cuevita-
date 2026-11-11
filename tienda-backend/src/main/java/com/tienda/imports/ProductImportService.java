package com.tienda.imports;

import com.tienda.categories.Category;
import com.tienda.categories.CategoryRepository;
import com.tienda.imports.ExcelCatalogReader.RawRow;
import com.tienda.imports.dto.ImportResult;
import com.tienda.imports.dto.ImportResult.Action;
import com.tienda.imports.dto.ImportResult.CategorySummary;
import com.tienda.imports.dto.ImportResult.ImportItem;
import com.tienda.imports.dto.ImportResult.Level;
import com.tienda.imports.dto.ImportResult.RowIssue;
import com.tienda.inventory.Inventory;
import com.tienda.inventory.InventoryRepository;
import com.tienda.inventory.InventoryService;
import com.tienda.inventory.MovementType;
import com.tienda.inventory.dto.MovementRequest;
import com.tienda.products.Product;
import com.tienda.products.ProductRepository;
import com.tienda.products.ProductStatus;
import com.tienda.products.variant.Color;
import com.tienda.products.variant.ColorRepository;
import com.tienda.products.variant.ProductVariant;
import com.tienda.products.variant.ProductVariantRepository;
import com.tienda.products.variant.Size;
import com.tienda.products.variant.SizeRepository;
import com.tienda.products.variant.VariantStatus;
import com.tienda.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private static final Pattern SIZE_IN_NAME = Pattern
            .compile("(?i)^(.*?)[\\s-]*TALLA\\s*(XXL|XL|XS|S|M|L|\\d{1,2})\\s*$");
    private static final String DEFAULT_SIZE = "Única";
    private static final String DEFAULT_COLOR = "Único";
    private static final String DEFAULT_COLOR_HEX = "#9CA3AF";

    private final ExcelCatalogReader reader;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    @Transactional
    public ImportResult run(MultipartFile file, boolean dryRun, User user) {
        String fileName = file.getOriginalFilename() == null ? "archivo" : file.getOriginalFilename();
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (file.isEmpty() || !(lower.endsWith(".xlsx") || lower.endsWith(".xls"))) {
            throw new IllegalArgumentException("Sube un archivo Excel (.xlsx o .xls).");
        }

        List<RawRow> rows;
        try {
            rows = reader.read(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo.");
        }

        List<RowIssue> issues = new ArrayList<>();
        List<ImportItem> items = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();
        Set<String> seenCombos = new HashSet<>();

        for (RawRow raw : rows) {
            int row = raw.rowNumber();
            String sku = raw.code().trim();

            if (sku.isEmpty()) {
                issues.add(new RowIssue(row, Level.ERROR, "Falta el código."));
                continue;
            }
            if (sku.length() > 60) {
                issues.add(new RowIssue(row, Level.ERROR, "El código supera los 60 caracteres."));
                continue;
            }
            if (raw.name().isBlank()) {
                issues.add(new RowIssue(row, Level.ERROR, "Falta el nombre del producto."));
                continue;
            }
            if (raw.price() == null || raw.price().signum() <= 0) {
                issues.add(new RowIssue(row, Level.ERROR, "Precio inválido: Pvp1 debe ser mayor a 0 (" + sku + ")."));
                continue;
            }
            if (!seenSkus.add(sku)) {
                issues.add(new RowIssue(row, Level.ERROR, "El código " + sku + " está repetido en el archivo."));
                continue;
            }
            if (TextCleaner.isDamaged(raw.name()) || TextCleaner.isDamaged(raw.category())) {
                issues.add(new RowIssue(row, Level.WARNING, "Se corrigieron caracteres dañados en el texto."));
            }

            String cleaned = TextCleaner.clean(raw.name());
            String base = cleaned;
            String size = DEFAULT_SIZE;
            Matcher matcher = SIZE_IN_NAME.matcher(cleaned);
            if (matcher.matches() && !matcher.group(1).isBlank()) {
                base = matcher.group(1).trim();
                size = matcher.group(2).toUpperCase(Locale.ROOT);
            }
            String name = TextCleaner.titleCase(base);
            if (name.length() > 150) {
                name = name.substring(0, 150);
            }

            int stock = raw.stock() == null ? 0 : raw.stock().intValue();
            if (stock < 0) {
                issues.add(new RowIssue(row, Level.WARNING, "Stock negativo en Codesoft; se cargará como 0."));
                stock = 0;
            }

            String category = CategoryNormalizer.normalize(raw.category());
            Optional<ProductVariant> existing = variantRepository.findBySku(sku);
            int currentStock = existing
                    .flatMap(variant -> inventoryRepository.findByVariantId(variant.getId()))
                    .map(Inventory::getQuantityOnHand)
                    .orElse(0);

            if (existing.isEmpty()) {
                if (!seenCombos.add(name.toLowerCase(Locale.ROOT) + "|" + size)) {
                    issues.add(new RowIssue(row, Level.ERROR,
                            "Otra fila ya define \"" + name + "\" en talla " + size + "."));
                    continue;
                }
                Optional<Product> product = productRepository.findFirstByNameIgnoreCase(name);
                final String sizeLabel = size;
                if (product.isPresent() && variantRepository.findByProductId(product.get().getId()).stream()
                        .anyMatch(variant -> variant.getSize().getLabel().equalsIgnoreCase(sizeLabel))) {
                    issues.add(new RowIssue(row, Level.ERROR,
                            "El producto \"" + name + "\" ya tiene una variante en talla " + size + "."));
                    continue;
                }
            }

            items.add(new ImportItem(row, sku, name, category, size, raw.price(), raw.cost(), stock, currentStock,
                    existing.isPresent() ? Action.UPDATE : Action.CREATE));
        }

        if (!dryRun) {
            apply(items, "IMPORT-" + fileName, user);
        }

        int created = (int) items.stream().filter(i -> i.action() == Action.CREATE).count();
        int updated = items.size() - created;
        return new ImportResult(dryRun, fileName, rows.size(), created, updated,
                rows.size() - items.size(), summarize(items), items, issues);
    }

    private List<CategorySummary> summarize(List<ImportItem> items) {
        Map<String, Integer> counts = new TreeMap<>();
        for (ImportItem item : items) {
            counts.merge(item.category(), 1, Integer::sum);
        }
        List<CategorySummary> summary = new ArrayList<>();
        counts.forEach((name, count) -> summary.add(new CategorySummary(
                name, count, categoryRepository.findBySlug(TextCleaner.slug(name)).isEmpty())));
        return summary;
    }

    private void apply(List<ImportItem> items, String reference, User user) {
        Map<String, Category> categories = new HashMap<>();
        Map<String, Product> products = new HashMap<>();

        for (ImportItem item : items) {
            boolean created = item.action() == Action.CREATE;
            ProductVariant variant;

            if (!created) {
                variant = variantRepository.findBySku(item.sku()).orElseThrow();
                variant.setPrice(item.price());
                if (item.cost() != null) {
                    variant.setCost(item.cost());
                }
                variant.setStatus(VariantStatus.ACTIVE);
                variantRepository.save(variant);
            } else {
                Category category = categories.computeIfAbsent(item.category(), this::findOrCreateCategory);
                Product product = products.computeIfAbsent(item.name().toLowerCase(Locale.ROOT),
                        key -> productRepository.findFirstByNameIgnoreCase(item.name())
                                .orElseGet(() -> productRepository.save(Product.builder()
                                        .category(category)
                                        .name(item.name())
                                        .baseStatus(ProductStatus.ACTIVE)
                                        .build())));
                variant = variantRepository.save(ProductVariant.builder()
                        .product(product)
                        .size(findOrCreateSize(item.size()))
                        .color(findOrCreateDefaultColor())
                        .sku(item.sku())
                        .barcode(item.sku())
                        .price(item.price())
                        .cost(item.cost())
                        .status(VariantStatus.ACTIVE)
                        .build());
                inventoryService.initializeForVariant(variant);
            }
            syncStock(variant, item.stock(), reference, user);
        }
    }

    private void syncStock(ProductVariant variant, int target, String reference, User user) {
        int current = inventoryRepository.findByVariantId(variant.getId())
                .map(Inventory::getQuantityOnHand)
                .orElse(0);
        int delta = target - current;
        if (delta == 0) {
            return;
        }
        boolean initial = current == 0 && delta > 0;
        inventoryService.registerMovement(new MovementRequest(
                variant.getId(),
                initial ? MovementType.ENTRADA : MovementType.AJUSTE,
                delta,
                initial ? "Carga inicial Codesoft" : "Sincronización Codesoft",
                reference,
                null), user);
    }

    private Category findOrCreateCategory(String name) {
        String slug = TextCleaner.slug(name);
        return categoryRepository.findBySlug(slug).orElseGet(() -> {
            String group = CategoryNormalizer.group(name);
            Category parent = group == null ? null : findOrCreateRootCategory(group);
            return categoryRepository.save(Category.builder().name(name).slug(slug).parent(parent).build());
        });
    }

    private Category findOrCreateRootCategory(String name) {
        String slug = TextCleaner.slug(name);
        return categoryRepository.findBySlug(slug)
                .orElseGet(() -> categoryRepository.save(Category.builder().name(name).slug(slug).build()));
    }

    private Size findOrCreateSize(String label) {
        return sizeRepository.findByLabel(label)
                .orElseGet(() -> sizeRepository.save(Size.builder().label(label).build()));
    }

    private Color findOrCreateDefaultColor() {
        return colorRepository.findByName(DEFAULT_COLOR)
                .orElseGet(
                        () -> colorRepository.save(Color.builder().name(DEFAULT_COLOR).hex(DEFAULT_COLOR_HEX).build()));
    }
}