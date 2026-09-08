package com.tiffincraft.app.utils;

import com.tiffincraft.app.models.Category;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** One canonical category vocabulary shared by customer discovery and cook forms. */
public final class MealCategoryCatalog {
    public static final String ALL = "all";
    public static final String[] SLUGS = {
            "nepali", "fast_food", "healthy", "snacks", "lunch",
            "desserts", "breakfast", "dinner", "beverages"
    };
    public static final String[] LABELS = {
            "Nepali", "Fast Food", "Healthy", "Snacks", "Lunch",
            "Desserts", "Breakfast", "Dinner", "Beverages"
    };
    public static final String[] EMOJIS = {
            "🍛", "🍕", "🥗", "🥟", "🥘", "🍰", "🍳", "🍽️", "🥤"
    };

    private MealCategoryCatalog() {}

    public static List<Category> homeCategories() {
        List<Category> result = new ArrayList<>();
        Category all = new Category(ALL, "🍽️", "All");
        all.setSelected(true);
        result.add(all);
        for (int i = 0; i < SLUGS.length; i++) {
            result.add(new Category(SLUGS[i], EMOJIS[i], LABELS[i]));
        }
        return result;
    }

    public static String labelFor(String slug) {
        for (int i = 0; i < SLUGS.length; i++) {
            if (SLUGS[i].equals(slug)) return LABELS[i];
        }
        return slug;
    }

    public static String primaryLegacyLabel(Collection<String> slugs) {
        if (slugs != null) {
            for (String slug : SLUGS) {
                if (slugs.contains(slug)) return labelFor(slug);
            }
        }
        return "Lunch";
    }

    public static String selectionSummary(Collection<String> slugs) {
        if (slugs == null || slugs.isEmpty()) return "Select categories";
        if (slugs.size() > 2) return slugs.size() + " categories selected";
        StringBuilder summary = new StringBuilder();
        for (String slug : SLUGS) {
            if (!slugs.contains(slug)) continue;
            if (summary.length() > 0) summary.append(", ");
            summary.append(labelFor(slug));
        }
        return summary.length() > 0 ? summary.toString() : "Select categories";
    }

    /** Compatibility for meals saved before category_slugs existed. */
    public static Set<String> fromLegacy(String category, String cuisineType) {
        Set<String> result = new LinkedHashSet<>();
        String value = category == null ? "" : category.toLowerCase(Locale.US);
        String cuisine = cuisineType == null ? "" : cuisineType.toLowerCase(Locale.US);
        if (value.contains("nepal") || cuisine.contains("nepal")) result.add("nepali");
        if (value.contains("fast food") || value.contains("fast_food")) result.add("fast_food");
        if (value.contains("healthy")) result.add("healthy");
        if (value.contains("snack")) result.add("snacks");
        if (value.contains("lunch")) result.add("lunch");
        if (value.contains("dessert")) result.add("desserts");
        if (value.contains("breakfast")) result.add("breakfast");
        if (value.contains("dinner")) result.add("dinner");
        if (value.contains("beverage")) result.add("beverages");
        return result;
    }
}
