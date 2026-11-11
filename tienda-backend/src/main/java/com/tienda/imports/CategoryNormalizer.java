package com.tienda.imports;

import java.util.Map;

/**
 * Unifica las categorías sucias de Codesoft. Edita este mapa para cambiar el
 * resultado.
 */
final class CategoryNormalizer {

    private static final Map<String, String> MAP = Map.ofEntries(
            Map.entry("BLUZA", "Blusas"),
            Map.entry("BLUSA", "Blusas"),
            Map.entry("VESTIDOS", "Vestidos"),
            Map.entry("VESTIDO", "Vestidos"),
            Map.entry("PANTALON DE MUJER", "Pantalones"),
            Map.entry("PANTALON JEAN MUJER", "Pantalones"),
            Map.entry("SHORE DE MUJER", "Shorts"),
            Map.entry("FALDA MUJER", "Faldas"),
            Map.entry("FALDA SHORE", "Faldas"),
            Map.entry("BERMUDA JEAN MUJER", "Bermudas"),
            Map.entry("CAMISA MANGA CORTA", "Camisas hombre"),
            Map.entry("CAMISA MANGA CORTA HOMBRE", "Camisas hombre"),
            Map.entry("CAMISA MUJER", "Camisas mujer"),
            Map.entry("CONJUNTO LINA", "Conjuntos"),
            Map.entry("CONJUNTO CORTO MUJER", "Conjuntos"),
            Map.entry("CONJUNTO DE HILO TACTO", "Conjuntos"),
            Map.entry("ENTERIZO", "Enterizos"),
            Map.entry("ENTERIZO MUJER", "Enterizos"),
            Map.entry("BLAZER DE MUJER", "Blazers"),
            Map.entry("CAMISETA MUJER", "Camisetas"),
            Map.entry("CAMISETA GALLETA", "Camisetas"),
            Map.entry("BODY MUJER", "Bodys"),
            Map.entry("CHALECO MUJER", "Chalecos"),
            Map.entry("BILLETERAS", "Billeteras"),
            Map.entry("BOLSOS CHINOS", "Carteras y bolsos"),
            Map.entry("SUETER MUJER", "Suéteres"),
            Map.entry("CHAQUETA MUJER", "Chaquetas"),
            Map.entry("CHOMPA", "Chaquetas"),
            Map.entry("CINTURONES", "Cinturones"),
            Map.entry("FAJA MUJER", "Fajas"),
            Map.entry("D PRAT", "Otros"));

    /** Grupo principal de cada categoría limpia. Las que no estén aquí quedan sin padre y se asignan desde el panel. */
    private static final Map<String, String> GROUPS = Map.ofEntries(
            Map.entry("Camisas hombre", "Hombre"),
            Map.entry("Billeteras", "Accesorios"),
            Map.entry("Carteras y bolsos", "Accesorios"),
            Map.entry("Cinturones", "Accesorios"),
            Map.entry("Blusas", "Mujer"),
            Map.entry("Vestidos", "Mujer"),
            Map.entry("Pantalones", "Mujer"),
            Map.entry("Shorts", "Mujer"),
            Map.entry("Bermudas", "Mujer"),
            Map.entry("Faldas", "Mujer"),
            Map.entry("Bodys", "Mujer"),
            Map.entry("Blazers", "Mujer"),
            Map.entry("Enterizos", "Mujer"),
            Map.entry("Conjuntos", "Mujer"),
            Map.entry("Chalecos", "Mujer"),
            Map.entry("Chaquetas", "Mujer"),
            Map.entry("Suéteres", "Mujer"),
            Map.entry("Camisas mujer", "Mujer"),
            Map.entry("Camisetas", "Mujer"),
            Map.entry("Fajas", "Mujer")
    );

    /** Nombre de la categoría padre (Mujer, Hombre, Accesorios) o null si no se conoce. */
    static String group(String categoryName) {
        return GROUPS.get(categoryName);
    }

    private CategoryNormalizer() {
    }

    static String normalize(String raw) {
        String key = TextCleaner.key(raw);
        if (key.isBlank()) {
            return "Otros";
        }
        String mapped = MAP.get(key);
        if (mapped != null) {
            return mapped;
        }
        if (key.startsWith("CARTERA")) {
            return "Carteras y bolsos";
        }
        return TextCleaner.titleCase(TextCleaner.clean(raw));
    }
}