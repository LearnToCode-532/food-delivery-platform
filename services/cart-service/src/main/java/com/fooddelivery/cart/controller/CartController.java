package com.fooddelivery.cart.controller;

import com.fooddelivery.cart.dto.CartResponse;
import com.fooddelivery.cart.dto.QuantityRequest;
import com.fooddelivery.cart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;

    @PutMapping("/cart")
    public ResponseEntity<CartResponse> createOrGet(
            @PathVariable @Min(1) Long userId) {
        return ResponseEntity.ok(cartService.createOrGet(userId));
    }

    @GetMapping("/cart")
    public ResponseEntity<CartResponse> getByUserId(
            @PathVariable @Min(1) Long userId) {
        return ResponseEntity.ok(cartService.getByUserId(userId));
    }

    @GetMapping("/cart/{cartId}")
    public ResponseEntity<CartResponse> getById(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long cartId) {
        return ResponseEntity.ok(cartService.getByIdAndUserId(cartId, userId));
    }

    @PostMapping("/cart/{cartId}/items/{menuItemId}")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long cartId,
            @PathVariable @Min(1) Long menuItemId,
            @Valid @RequestBody QuantityRequest request) {
        return ResponseEntity.ok(cartService.addItem(
                userId, cartId, menuItemId, request.quantity().intValueExact()));
    }

    @PutMapping("/cart/{cartId}/items/{menuItemId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long cartId,
            @PathVariable @Min(1) Long menuItemId,
            @Valid @RequestBody QuantityRequest request) {
        return ResponseEntity.ok(cartService.updateQuantity(
                userId, cartId, menuItemId, request.quantity().intValueExact()));
    }

    @DeleteMapping("/cart/{cartId}/items/{menuItemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long cartId,
            @PathVariable @Min(1) Long menuItemId) {
        cartService.removeItem(userId, cartId, menuItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cart/{cartId}/items")
    public ResponseEntity<Void> clear(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long cartId) {
        cartService.clear(userId, cartId);
        return ResponseEntity.noContent().build();
    }
}
