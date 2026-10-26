package com.fooddelivery.catalog.restaurant.service;

import com.fooddelivery.catalog.dto.PageResponse;
import com.fooddelivery.catalog.restaurant.dto.CreateRestaurantRequest;
import com.fooddelivery.catalog.restaurant.dto.RestaurantResponse;
import com.fooddelivery.catalog.restaurant.dto.UpdateRestaurantRequest;

public interface RestaurantService {

    RestaurantResponse create(CreateRestaurantRequest request);

    RestaurantResponse getById(Long id);

    PageResponse<RestaurantResponse> list(int page, int size, String sortBy, String direction);

    RestaurantResponse update(Long id, UpdateRestaurantRequest request);

    RestaurantResponse setActive(Long id, boolean active);
}
