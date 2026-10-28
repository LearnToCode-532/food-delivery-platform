package com.fooddelivery.cart.service;

import com.fooddelivery.cart.catalog.CatalogMenuItem;
import com.fooddelivery.cart.catalog.CatalogMenuItemClient;
import com.fooddelivery.cart.dto.CartResponse;
import com.fooddelivery.cart.entity.Cart;
import com.fooddelivery.cart.entity.CartItem;
import com.fooddelivery.cart.exception.CatalogServiceUnavailableException;
import com.fooddelivery.cart.exception.CartNotFoundException;
import com.fooddelivery.cart.exception.InvalidCartRequestException;
import com.fooddelivery.cart.service.impl.CartServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartPersistenceService cartPersistenceService;

    @Mock
    private CatalogMenuItemClient catalogMenuItemClient;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void calculatesSubtotalFromPersistedPriceSnapshotUsingExactDecimalArithmetic() {
        Cart cart = cart();
        cart.addItem(item(cart, 31L, 3, "0.10"));
        cart.addItem(item(cart, 32L, 2, "12.34"));
        when(cartPersistenceService.getByUserId(5L)).thenReturn(cart);

        CartResponse response = cartService.getByUserId(5L);

        assertEquals(new BigDecimal("24.98"), response.subtotal());
        assertEquals(new BigDecimal("0.30"), response.items().get(0).lineTotal());
        assertEquals("INR", response.currency());
    }

    @Test
    void obtainsCatalogPriceAndDoesNotCallCatalogForInvalidQuantity() {
        CatalogMenuItem menuItem = new CatalogMenuItem(31L, "Soup", new BigDecimal("12.50"), "INR", true);
        when(catalogMenuItemClient.getMenuItem(31L)).thenReturn(menuItem);
        when(cartPersistenceService.addItem(5L, 11L, menuItem, 2)).thenReturn(cartWith(menuItem, 2));

        CartResponse response = cartService.addItem(5L, 11L, 31L, 2);

        assertEquals(new BigDecimal("25.00"), response.subtotal());
        verify(catalogMenuItemClient).getMenuItem(31L);

        assertThrows(InvalidCartRequestException.class,
                () -> cartService.addItem(5L, 11L, 31L, 0));
        verify(catalogMenuItemClient, times(1)).getMenuItem(31L);
    }

    @Test
    void catalogFailuresPropagateWithoutChangingCart() {
        when(catalogMenuItemClient.getMenuItem(31L)).thenThrow(new CatalogServiceUnavailableException());

        assertThrows(CatalogServiceUnavailableException.class,
                () -> cartService.addItem(5L, 11L, 31L, 1));
        verify(cartPersistenceService, never()).addItem(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void checksCartOwnershipBeforeCallingCatalog() {
        doThrow(new CartNotFoundException())
                .when(cartPersistenceService).verifyOwnership(11L, 99L);

        assertThrows(CartNotFoundException.class,
                () -> cartService.addItem(99L, 11L, 31L, 1));

        verifyNoInteractions(catalogMenuItemClient);
    }

    private Cart cart() {
        return Cart.builder()
                .id(11L)
                .userId(5L)
                .items(new ArrayList<>())
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private Cart cartWith(CatalogMenuItem menuItem, int quantity) {
        Cart cart = cart();
        cart.addItem(item(cart, menuItem.id(), quantity, menuItem.price().toPlainString()));
        return cart;
    }

    private CartItem item(Cart cart, Long menuItemId, int quantity, String price) {
        return CartItem.builder()
                .id(menuItemId)
                .cart(cart)
                .menuItemId(menuItemId)
                .menuItemName("Item " + menuItemId)
                .quantity(quantity)
                .unitPrice(new BigDecimal(price))
                .currency("INR")
                .build();
    }
}
