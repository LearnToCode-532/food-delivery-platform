package com.fooddelivery.catalog.menu.service;

import com.fooddelivery.catalog.dto.PageResponse;
import com.fooddelivery.catalog.menu.dto.CreateMenuItemRequest;
import com.fooddelivery.catalog.menu.dto.MenuItemResponse;
import com.fooddelivery.catalog.menu.dto.UpdateMenuItemRequest;

public interface MenuItemService {

    MenuItemResponse create(Long restaurantId, CreateMenuItemRequest request);

    PageResponse<MenuItemResponse> listForRestaurant(
            Long restaurantId,
            int page,
            int size,
            String sortBy,
            String direction);

    MenuItemResponse getById(Long id);

    MenuItemResponse update(Long id, UpdateMenuItemRequest request);

    MenuItemResponse setAvailable(Long id, boolean available);

    void softDelete(Long id);
}
