package com.fooddelivery.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;

public record QuantityRequest(
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        @Digits(integer = 10, fraction = 0, message = "quantity must be a positive integer")
        @DecimalMax(value = "2147483647", message = "quantity exceeds the supported maximum")
        BigDecimal quantity) {
}
