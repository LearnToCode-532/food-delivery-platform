package com.fooddelivery.order.service.impl;

import com.fooddelivery.order.client.CartClient;
import com.fooddelivery.order.client.CartItemResponse;
import com.fooddelivery.order.client.CartResponse;
import com.fooddelivery.order.dto.OrderItemResponse;
import com.fooddelivery.order.dto.OrderResponse;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.exception.EmptyCartException;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartClient cartClient;

    public OrderServiceImpl(OrderRepository orderRepository, CartClient cartClient) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
    }

    @Override
    public OrderResponse create(Long userId, Long cartId) {
        CartResponse cart = cartClient.getCart(userId, cartId);
        if (cart.items() == null || cart.items().isEmpty()) {
            throw new EmptyCartException();
        }

        BigDecimal subtotal = cart.items().stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);

        Order order = Order.builder()
                .userId(userId)
                .cartId(cartId)
                .status(OrderStatus.CREATED)
                .subtotal(subtotal)
                .currency(cart.currency())
                .build();

        cart.items().stream()
                .sorted(Comparator.comparing(CartItemResponse::menuItemId))
                .forEach(item -> order.addItem(toOrderItem(item, cart.currency())));

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    private OrderItem toOrderItem(CartItemResponse item, String fallbackCurrency) {
        String currency = item.currency() == null ? fallbackCurrency : item.currency();
        return OrderItem.builder()
                .menuItemId(item.menuItemId())
                .menuItemName(item.menuItemName())
                .quantity(item.quantity())
                .unitPrice(item.unitPrice().setScale(2, RoundingMode.UNNECESSARY))
                .lineTotal(item.lineTotal().setScale(2, RoundingMode.UNNECESSARY))
                .currency(currency)
                .build();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getMenuItemId))
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getMenuItemId(),
                        item.getMenuItemName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal(),
                        item.getCurrency()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getCartId(),
                order.getStatus().name(),
                items,
                order.getSubtotal(),
                order.getCurrency(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
