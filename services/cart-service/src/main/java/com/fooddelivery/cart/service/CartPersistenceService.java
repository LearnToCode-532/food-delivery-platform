package com.fooddelivery.cart.service;

import com.fooddelivery.cart.catalog.CatalogMenuItem;
import com.fooddelivery.cart.entity.Cart;
import com.fooddelivery.cart.entity.CartItem;
import com.fooddelivery.cart.exception.CartItemNotFoundException;
import com.fooddelivery.cart.exception.CartNotFoundException;
import com.fooddelivery.cart.exception.InvalidCartRequestException;
import com.fooddelivery.cart.exception.MenuItemUnavailableException;
import com.fooddelivery.cart.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class CartPersistenceService {

    private final CartRepository cartRepository;

    public CartPersistenceService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Transactional
    public Cart createOrGet(Long userId) {
        return cartRepository.findWithItemsByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
    }

    @Transactional(readOnly = true)
    public Cart getByUserId(Long userId) {
        return cartRepository.findWithItemsByUserId(userId)
                .orElseThrow(CartNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Cart getByIdAndUserId(Long cartId, Long userId) {
        return findCart(cartId, userId);
    }

    @Transactional(readOnly = true)
    public void verifyOwnership(Long cartId, Long userId) {
        if (!cartRepository.existsByIdAndUserId(cartId, userId)) {
            throw new CartNotFoundException();
        }
    }

    @Transactional
    public Cart addItem(Long userId, Long cartId, CatalogMenuItem menuItem, Integer quantity) {
        validateQuantity(quantity);
        if (!menuItem.available()) {
            throw new MenuItemUnavailableException(menuItem.id());
        }

        Cart cart = findCart(cartId, userId);
        BigDecimal unitPrice = menuItem.price().setScale(2, RoundingMode.UNNECESSARY);
        CartItem existing = cart.findItem(menuItem.id()).orElse(null);
        if (existing == null) {
            cart.addItem(CartItem.builder()
                    .menuItemId(menuItem.id())
                    .menuItemName(menuItem.name())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .currency(menuItem.currency())
                    .build());
        } else {
            try {
                existing.setQuantity(Math.addExact(existing.getQuantity(), quantity));
            } catch (ArithmeticException exception) {
                throw new InvalidCartRequestException("combined quantity exceeds the supported maximum");
            }
            existing.setMenuItemName(menuItem.name());
            existing.setUnitPrice(unitPrice);
            existing.setCurrency(menuItem.currency());
        }
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateQuantity(Long userId, Long cartId, Long menuItemId, Integer quantity) {
        validateQuantity(quantity);
        Cart cart = findCart(cartId, userId);
        CartItem item = cart.findItem(menuItemId)
                .orElseThrow(() -> new CartItemNotFoundException(menuItemId));
        item.setQuantity(quantity);
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    @Transactional
    public void removeItem(Long userId, Long cartId, Long menuItemId) {
        Cart cart = findCart(cartId, userId);
        CartItem item = cart.findItem(menuItemId)
                .orElseThrow(() -> new CartItemNotFoundException(menuItemId));
        cart.removeItem(item);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    @Transactional
    public void clear(Long userId, Long cartId) {
        Cart cart = findCart(cartId, userId);
        cart.clearItems();
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    private Cart findCart(Long cartId, Long userId) {
        return cartRepository.findWithItemsByIdAndUserId(cartId, userId)
                .orElseThrow(CartNotFoundException::new);
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new InvalidCartRequestException("quantity must be a positive integer");
        }
    }
}
