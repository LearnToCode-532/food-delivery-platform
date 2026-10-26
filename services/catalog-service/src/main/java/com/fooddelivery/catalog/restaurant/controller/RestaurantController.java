package com.fooddelivery.catalog.restaurant.controller;

import com.fooddelivery.catalog.config.CatalogPagination;
import com.fooddelivery.catalog.dto.PageResponse;
import com.fooddelivery.catalog.restaurant.dto.CreateRestaurantRequest;
import com.fooddelivery.catalog.restaurant.dto.RestaurantActivationRequest;
import com.fooddelivery.catalog.restaurant.dto.RestaurantResponse;
import com.fooddelivery.catalog.restaurant.dto.UpdateRestaurantRequest;
import com.fooddelivery.catalog.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
@Validated
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public ResponseEntity<RestaurantResponse> create(
            @Valid @RequestBody CreateRestaurantRequest request) {
        RestaurantResponse created = restaurantService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/restaurants/" + created.getId()))
                .body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getById(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(restaurantService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<RestaurantResponse>> list(
            @RequestParam(defaultValue = "" + CatalogPagination.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = "" + CatalogPagination.DEFAULT_SIZE) @Min(1) @Max(CatalogPagination.MAX_SIZE) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(restaurantService.list(page, size, sortBy, direction));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponse> update(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateRestaurantRequest request) {
        return ResponseEntity.ok(restaurantService.update(id, request));
    }

    @PatchMapping("/{id}/activation")
    public ResponseEntity<RestaurantResponse> setActive(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody RestaurantActivationRequest request) {
        return ResponseEntity.ok(restaurantService.setActive(id, request.getActive()));
    }
}
