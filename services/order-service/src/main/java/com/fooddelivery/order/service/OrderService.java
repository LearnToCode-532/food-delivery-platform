package com.fooddelivery.order.service;

import com.fooddelivery.order.dto.OrderResponse;

public interface OrderService {
    OrderResponse create(Long userId, Long cartId);
}
