package com.nucosmos.pos.backend.product;

public enum ProductCustomizationSelectionMode {
    SINGLE,
    MULTIPLE;

    public static ProductCustomizationSelectionMode from(String value) {
        return ProductCustomizationSelectionMode.valueOf(value.trim().toUpperCase());
    }
}
