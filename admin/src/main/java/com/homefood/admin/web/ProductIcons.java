package com.homefood.admin.web;

/**
 * Placeholder icon for a product card when it has no real photo (Product.imageUrl blank) - there's
 * no legitimate way to auto-source stock photos of a specific home cook's actual dishes, so this
 * picks a relevant emoji by keyword instead. Once a real photo is uploaded, imageUrl wins and this
 * is never consulted for that product.
 */
public final class ProductIcons {

    private ProductIcons() {
    }

    public static String iconFor(String productName) {
        if (productName == null) {
            return "🍽️";
        }
        String name = productName.toLowerCase();
        if (name.contains("пельмен") || name.contains("вареник") || name.contains("чебурек")) {
            return "🥟";
        }
        if (name.contains("капуст") || name.contains("голубц")) {
            return "🥬";
        }
        if (name.contains("морков")) {
            return "🥕";
        }
        if (name.contains("сырник")) {
            return "🧀";
        }
        if (name.contains("торт") || name.contains("медовик") || name.contains("наполеон")) {
            return "🎂";
        }
        return "🍽️";
    }
}
