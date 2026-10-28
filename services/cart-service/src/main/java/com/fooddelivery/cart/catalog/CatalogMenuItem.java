package com.fooddelivery.cart.catalog;

import java.math.BigDecimal;

public record CatalogMenuItem(
        Long id,
        String name,
        BigDecimal price,
        String currency,
        Boolean available) {
}
