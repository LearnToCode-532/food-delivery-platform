package com.fooddelivery.cart.service;

import com.fooddelivery.cart.catalog.CatalogMenuItem;
import com.fooddelivery.cart.entity.Cart;
import com.fooddelivery.cart.entity.CartItem;
import com.fooddelivery.cart.exception.CartItemNotFoundException;
import com.fooddelivery.cart.exception.CartNotFoundException;
import com.fooddelivery.cart.exception.InvalidCartRequestException;
import com.fooddelivery.cart.exception.MenuItemUnavailableException;
import com.fooddelivery.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartPersistenceServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartPersistenceService cartPersistenceService;

    @Test
    void createsCartForUserAndReturnsExistingCartOnRepeat() {
        Cart cart = cart(11L, 5L);
        when(cartRepository.findWithItemsByUserId(5L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Cart.class));

        Cart created = cartPersistenceService.createOrGet(5L);
        Cart retrieved = cartPersistenceService.createOrGet(5L);

        assertEquals(5L, created.getUserId());
        assertEquals(11L, retrieved.getId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addsNewLineAndIncrementsSameMenuItemInsteadOfDuplicating() {
        Cart cart = cart(11L, 5L);
        CatalogMenuItem menuItem = menuItem(31L, "Soup", "12.50");
        when(cartRepository.findWithItemsByIdAndUserId(11L, 5L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);

        cartPersistenceService.addItem(5L, 11L, menuItem, 2);
        cartPersistenceService.addItem(5L, 11L, menuItem(31L, "Soup of the day", "13.25"), 3);

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals("Soup of the day", cart.getItems().get(0).getMenuItemName());
        assertEquals(new BigDecimal("13.25"), cart.getItems().get(0).getUnitPrice());
    }

    @Test
    void rejectsUnavailableItemAndNonPositiveQuantityWithoutSaving() {
        assertThrows(MenuItemUnavailableException.class,
                () -> cartPersistenceService.addItem(5L, 11L,
                        new CatalogMenuItem(31L, "Soup", new BigDecimal("12.50"), "INR", false), 1));
        assertThrows(InvalidCartRequestException.class,
                () -> cartPersistenceService.addItem(5L, 11L,
                        menuItem(31L, "Soup", "12.50"), 0));

        verify(cartRepository, never()).findWithItemsByIdAndUserId(11L, 5L);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void rejectsCombinedQuantityAboveDatabaseIntegerRange() {
        Cart cart = cart(11L, 5L);
        cart.addItem(line(cart, 31L, Integer.MAX_VALUE, "12.50"));
        when(cartRepository.findWithItemsByIdAndUserId(11L, 5L)).thenReturn(Optional.of(cart));

        assertThrows(InvalidCartRequestException.class,
                () -> cartPersistenceService.addItem(5L, 11L,
                        menuItem(31L, "Soup", "12.50"), 1));

        assertEquals(Integer.MAX_VALUE, cart.getItems().get(0).getQuantity());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updatesQuantityRemovesLineAndClearsCart() {
        Cart cart = cart(11L, 5L);
        cart.addItem(line(cart, 31L, 2, "12.50"));
        when(cartRepository.findWithItemsByIdAndUserId(11L, 5L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);

        Cart updated = cartPersistenceService.updateQuantity(5L, 11L, 31L, 4);
        assertEquals(4, updated.getItems().get(0).getQuantity());

        cartPersistenceService.removeItem(5L, 11L, 31L);
        assertEquals(0, cart.getItems().size());

        cart.addItem(line(cart, 31L, 1, "12.50"));
        cartPersistenceService.clear(5L, 11L);
        assertEquals(0, cart.getItems().size());
    }

    @Test
    void wrongOwnerMissingCartAndMissingLineAreNotFound() {
        when(cartRepository.findWithItemsByIdAndUserId(11L, 99L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class,
                () -> cartPersistenceService.getByIdAndUserId(11L, 99L));
        assertThrows(CartNotFoundException.class,
                () -> cartPersistenceService.updateQuantity(99L, 11L, 31L, 1));

        when(cartRepository.findWithItemsByUserId(99L)).thenReturn(Optional.empty());
        assertThrows(CartNotFoundException.class, () -> cartPersistenceService.getByUserId(99L));

        Cart cart = cart(11L, 5L);
        when(cartRepository.findWithItemsByIdAndUserId(11L, 5L)).thenReturn(Optional.of(cart));
        assertThrows(CartItemNotFoundException.class,
                () -> cartPersistenceService.removeItem(5L, 11L, 31L));
        assertThrows(CartNotFoundException.class,
                () -> cartPersistenceService.verifyOwnership(11L, 99L));
    }

    private Cart cart(Long id, Long userId) {
        return Cart.builder()
                .id(id)
                .userId(userId)
                .items(new ArrayList<>())
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private CartItem line(Cart cart, Long menuItemId, int quantity, String price) {
        return CartItem.builder()
                .cart(cart)
                .menuItemId(menuItemId)
                .menuItemName("Soup")
                .quantity(quantity)
                .unitPrice(new BigDecimal(price))
                .currency("INR")
                .build();
    }

    private CatalogMenuItem menuItem(Long id, String name, String price) {
        return new CatalogMenuItem(id, name, new BigDecimal(price), "INR", true);
    }
}
