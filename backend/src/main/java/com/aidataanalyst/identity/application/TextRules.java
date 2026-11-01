package com.aidataanalyst.identity.application;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextRules {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private TextRules() {
    }

    public static String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static String slugFromName(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = NON_SLUG.matcher(normalized).replaceAll("-").replaceAll("^-+|-+$", "");
        if (slug.length() > 48) {
            slug = slug.substring(0, 48).replaceAll("-+$", "");
        }
        if (slug.length() < 3) {
            slug = "workspace";
        }
        return slug;
    }

    public static boolean isSlug(String value) {
        return value != null && value.length() >= 3 && value.length() <= 48 && SLUG.matcher(value).matches();
    }

    public static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
