package com.fooddelivery.catalog.restaurant.service.impl;

import com.fooddelivery.catalog.config.CatalogPagination;
import com.fooddelivery.catalog.dto.PageResponse;
import com.fooddelivery.catalog.exception.CatalogNotFoundException;
import com.fooddelivery.catalog.restaurant.dto.CreateRestaurantRequest;
import com.fooddelivery.catalog.restaurant.dto.RestaurantResponse;
import com.fooddelivery.catalog.restaurant.dto.UpdateRestaurantRequest;
import com.fooddelivery.catalog.restaurant.entity.Restaurant;
import com.fooddelivery.catalog.restaurant.repository.RestaurantRepository;
import com.fooddelivery.catalog.restaurant.service.RestaurantService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class RestaurantServiceImpl implements RestaurantService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "createdAt", "updatedAt", "active");

    private final RestaurantRepository restaurantRepository;

    public RestaurantServiceImpl(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    public RestaurantResponse create(CreateRestaurantRequest request) {
        Restaurant restaurant = Restaurant.builder()
                .name(requiredText(request.getName()))
                .description(optionalText(request.getDescription()))
                .address(optionalText(request.getAddress()))
                .phoneNumber(optionalText(request.getPhoneNumber()))
                .active(true)
                .build();
        return toResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getById(Long id) {
        return toResponse(findRestaurant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> list(int page, int size, String sortBy, String direction) {
        Page<Restaurant> restaurants = restaurantRepository.findAll(
                CatalogPagination.create(page, size, sortBy, direction, SORT_FIELDS));
        return PageResponse.from(restaurants, this::toResponse);
    }

    @Override
    public RestaurantResponse update(Long id, UpdateRestaurantRequest request) {
        Restaurant restaurant = findRestaurant(id);
        restaurant.setName(requiredText(request.getName()));
        restaurant.setDescription(optionalText(request.getDescription()));
        restaurant.setAddress(optionalText(request.getAddress()));
        restaurant.setPhoneNumber(optionalText(request.getPhoneNumber()));
        return toResponse(restaurantRepository.save(restaurant));
    }

    @Override
    public RestaurantResponse setActive(Long id, boolean active) {
        Restaurant restaurant = findRestaurant(id);
        restaurant.setActive(active);
        return toResponse(restaurantRepository.save(restaurant));
    }

    private Restaurant findRestaurant(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> CatalogNotFoundException.restaurant(id));
    }

    private String requiredText(String value) {
        return value.trim();
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .address(restaurant.getAddress())
                .phoneNumber(restaurant.getPhoneNumber())
                .active(restaurant.isActive())
                .createdAt(restaurant.getCreatedAt())
                .updatedAt(restaurant.getUpdatedAt())
                .build();
    }
}
