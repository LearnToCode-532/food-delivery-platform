package com.fooddelivery.order;

import com.fooddelivery.order.client.CartClient;
import com.fooddelivery.order.client.CartItemResponse;
import com.fooddelivery.order.client.CartResponse;
import com.fooddelivery.order.dto.OrderResponse;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.order.exception.CartNotFoundException;
import com.fooddelivery.order.exception.EmptyCartException;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private CartClient cartClient;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createsOrderFromCartAndPersistsSnapshotValues() {
        CartResponse cart = cartResponse(5L, 11L, List.of(
                new CartItemResponse(1L, 101L, "Burger", 2, new BigDecimal("12.50"), new BigDecimal("25.00"), "INR"),
                new CartItemResponse(2L, 102L, "Fries", 3, new BigDecimal("3.20"), new BigDecimal("9.60"), "INR")));
        when(cartClient.getCart(5L, 11L)).thenReturn(cart);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(77L);
            order.getItems().forEach(item -> item.setId(item.getMenuItemId()));
            return order;
        });

        OrderResponse response = orderService.create(5L, 11L);

        assertEquals(77L, response.id());
        assertEquals(5L, response.userId());
        assertEquals(11L, response.cartId());
        assertEquals("CREATED", response.status());
        assertEquals(new BigDecimal("34.60"), response.subtotal());
        assertEquals("INR", response.currency());
        assertEquals(2, response.items().size());
        assertEquals(101L, response.items().get(0).menuItemId());
        assertEquals(new BigDecimal("25.00"), response.items().get(0).lineTotal());
        assertEquals(new BigDecimal("9.60"), response.items().get(1).lineTotal());
    }

    @Test
    void rejectsEmptyCartBeforeSavingOrder() {
        CartResponse cart = cartResponse(5L, 11L, List.of());
        when(cartClient.getCart(5L, 11L)).thenReturn(cart);

        assertThrows(EmptyCartException.class, () -> orderService.create(5L, 11L));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void rejectsMissingCartForUserAndCartPair() {
        when(cartClient.getCart(5L, 999L)).thenThrow(new CartNotFoundException(5L, 999L));

        assertThrows(CartNotFoundException.class, () -> orderService.create(5L, 999L));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void usesSnapshotPriceAndLineTotalsFromCartResponse() {
        CartResponse cart = cartResponse(5L, 11L, List.of(
                new CartItemResponse(3L, 200L, "Pizza", 1, new BigDecimal("7.50"), new BigDecimal("7.50"), "INR"),
                new CartItemResponse(4L, 201L, "Tea", 2, new BigDecimal("2.00"), new BigDecimal("4.00"), "INR")));
        when(cartClient.getCart(5L, 11L)).thenReturn(cart);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(44L);
            return order;
        });

        OrderResponse response = orderService.create(5L, 11L);

        assertEquals(new BigDecimal("11.50"), response.subtotal());
        assertEquals(new BigDecimal("7.50"), response.items().get(0).unitPrice());
        assertEquals(new BigDecimal("4.00"), response.items().get(1).lineTotal());
    }

    private CartResponse cartResponse(Long userId, Long cartId, List<CartItemResponse> items) {
        return new CartResponse(cartId, userId, items, items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add), "INR", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
