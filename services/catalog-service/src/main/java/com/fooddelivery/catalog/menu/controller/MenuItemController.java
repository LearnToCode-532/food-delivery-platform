package com.fooddelivery.catalog.menu.controller;

import com.fooddelivery.catalog.config.CatalogPagination;
import com.fooddelivery.catalog.dto.PageResponse;
import com.fooddelivery.catalog.menu.dto.CreateMenuItemRequest;
import com.fooddelivery.catalog.menu.dto.MenuItemAvailabilityRequest;
import com.fooddelivery.catalog.menu.dto.MenuItemResponse;
import com.fooddelivery.catalog.menu.dto.UpdateMenuItemRequest;
import com.fooddelivery.catalog.menu.service.MenuItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequiredArgsConstructor
@Validated
public class MenuItemController {

    private final MenuItemService menuItemService;

    @PostMapping("/api/v1/restaurants/{restaurantId}/menu-items")
    public ResponseEntity<MenuItemResponse> create(
            @PathVariable @Min(1) Long restaurantId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemResponse created = menuItemService.create(restaurantId, request);
        return ResponseEntity.created(URI.create("/api/v1/menu-items/" + created.getId()))
                .body(created);
    }

    @GetMapping("/api/v1/restaurants/{restaurantId}/menu-items")
    public ResponseEntity<PageResponse<MenuItemResponse>> listForRestaurant(
            @PathVariable @Min(1) Long restaurantId,
            @RequestParam(defaultValue = "" + CatalogPagination.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = "" + CatalogPagination.DEFAULT_SIZE) @Min(1) @Max(CatalogPagination.MAX_SIZE) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(menuItemService.listForRestaurant(
                restaurantId, page, size, sortBy, direction));
    }

    @GetMapping("/api/v1/menu-items/{id}")
    public ResponseEntity<MenuItemResponse> getById(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(menuItemService.getById(id));
    }

    @PutMapping("/api/v1/menu-items/{id}")
    public ResponseEntity<MenuItemResponse> update(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        return ResponseEntity.ok(menuItemService.update(id, request));
    }

    @PatchMapping("/api/v1/menu-items/{id}/availability")
    public ResponseEntity<MenuItemResponse> setAvailable(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody MenuItemAvailabilityRequest request) {
        return ResponseEntity.ok(menuItemService.setAvailable(id, request.getAvailable()));
    }

    @DeleteMapping("/api/v1/menu-items/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable @Min(1) Long id) {
        menuItemService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
