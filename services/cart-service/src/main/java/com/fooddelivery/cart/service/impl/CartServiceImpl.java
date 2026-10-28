package com.fooddelivery.cart.service.impl;

import com.fooddelivery.cart.catalog.CatalogMenuItem;
import com.fooddelivery.cart.catalog.CatalogMenuItemClient;
import com.fooddelivery.cart.dto.CartItemResponse;
import com.fooddelivery.cart.dto.CartResponse;
import com.fooddelivery.cart.entity.Cart;
import com.fooddelivery.cart.entity.CartItem;
import com.fooddelivery.cart.exception.InvalidCartRequestException;
import com.fooddelivery.cart.service.CartPersistenceService;
import com.fooddelivery.cart.service.CartService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private static final String CURRENCY = "INR";

    private final CartPersistenceService cartPersistenceService;
    private final CatalogMenuItemClient catalogMenuItemClient;

    public CartServiceImpl(
            CartPersistenceService cartPersistenceService,
            CatalogMenuItemClient catalogMenuItemClient) {
        this.cartPersistenceService = cartPersistenceService;
        this.catalogMenuItemClient = catalogMenuItemClient;
    }

    @Override
    public CartResponse createOrGet(Long userId) {
        return toResponse(cartPersistenceService.createOrGet(userId));
    }

    @Override
    public CartResponse getByUserId(Long userId) {
        return toResponse(cartPersistenceService.getByUserId(userId));
    }

    @Override
    public CartResponse getByIdAndUserId(Long cartId, Long userId) {
        return toResponse(cartPersistenceService.getByIdAndUserId(cartId, userId));
    }

    @Override
    public CartResponse addItem(Long userId, Long cartId, Long menuItemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new InvalidCartRequestException("quantity must be a positive integer");
        }
        cartPersistenceService.verifyOwnership(cartId, userId);
        CatalogMenuItem item = catalogMenuItemClient.getMenuItem(menuItemId);
        Cart cart = cartPersistenceService.addItem(userId, cartId, item, quantity);
        return toResponse(cart);
    }

    @Override
    public CartResponse updateQuantity(Long userId, Long cartId, Long menuItemId, Integer quantity) {
        return toResponse(cartPersistenceService.updateQuantity(userId, cartId, menuItemId, quantity));
    }

    @Override
    public void removeItem(Long userId, Long cartId, Long menuItemId) {
        cartPersistenceService.removeItem(userId, cartId, menuItemId);
    }

    @Override
    public void clear(Long userId, Long cartId) {
        cartPersistenceService.clear(userId, cartId);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getMenuItemId))
                .map(this::toItemResponse)
                .toList();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                items,
                subtotal,
                CURRENCY,
                cart.getCreatedAt(),
                cart.getUpdatedAt());
    }

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal unitPrice = item.getUnitPrice().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, RoundingMode.UNNECESSARY);
        return new CartItemResponse(
                item.getId(),
                item.getMenuItemId(),
                item.getMenuItemName(),
                item.getQuantity(),
                unitPrice,
                lineTotal,
                item.getCurrency());
    }
}
