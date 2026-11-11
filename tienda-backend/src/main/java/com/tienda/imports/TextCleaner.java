package com.tienda.imports;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class TextCleaner {

    private static final Locale ES = new Locale("es", "EC");
    private static final Pattern BROKEN_IN_WORD = Pattern.compile("(?<=\\p{L})\uFFFD(?=\\p{L})");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private TextCleaner() {
    }

    static boolean isDamaged(String raw) {
        return raw != null && raw.indexOf('\uFFFD') >= 0;
    }

    static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace("&amp;", "&").replace('\u00A0', ' ');
        text = BROKEN_IN_WORD.matcher(text).replaceAll("Ñ");
        text = text.replace("\uFFFD", "");
        return SPACES.matcher(text).replaceAll(" ").trim();
    }

    static String titleCase(String text) {
        StringBuilder out = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            boolean keepUpper = word.chars().anyMatch(c -> c == '&' || Character.isDigit(c));
            out.append(keepUpper
                    ? word.toUpperCase(ES)
                    : word.substring(0, 1).toUpperCase(ES) + word.substring(1).toLowerCase(ES));
        }
        return out.toString();
    }

    static String key(String text) {
        String plain = Normalizer.normalize(clean(text), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toUpperCase(ES);
    }

    static String slug(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return plain.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}