package com.fooddelivery.catalog.menu.service.impl;

import com.fooddelivery.catalog.config.CatalogPagination;
import com.fooddelivery.catalog.dto.PageResponse;
import com.fooddelivery.catalog.exception.CatalogNotFoundException;
import com.fooddelivery.catalog.exception.InvalidCatalogParameterException;
import com.fooddelivery.catalog.menu.dto.CreateMenuItemRequest;
import com.fooddelivery.catalog.menu.dto.MenuItemResponse;
import com.fooddelivery.catalog.menu.dto.UpdateMenuItemRequest;
import com.fooddelivery.catalog.menu.entity.MenuItem;
import com.fooddelivery.catalog.menu.repository.MenuItemRepository;
import com.fooddelivery.catalog.menu.service.MenuItemService;
import com.fooddelivery.catalog.restaurant.entity.Restaurant;
import com.fooddelivery.catalog.restaurant.repository.RestaurantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Set;

@Service
@Transactional
public class MenuItemServiceImpl implements MenuItemService {

    private static final String CURRENCY = "INR";
    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "createdAt", "updatedAt", "price");

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuItemServiceImpl(
            MenuItemRepository menuItemRepository,
            RestaurantRepository restaurantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    public MenuItemResponse create(Long restaurantId, CreateMenuItemRequest request) {
        BigDecimal price = normalizePrice(request.getPrice());
        Restaurant restaurant = findRestaurant(restaurantId);
        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .name(requiredText(request.getName()))
                .description(optionalText(request.getDescription()))
                .price(price)
                .available(true)
                .build();
        return toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MenuItemResponse> listForRestaurant(
            Long restaurantId,
            int page,
            int size,
            String sortBy,
            String direction) {
        Restaurant restaurant = findRestaurant(restaurantId);
        Pageable pageable = CatalogPagination.create(page, size, sortBy, direction, SORT_FIELDS);
        if (!restaurant.isActive()) {
            return PageResponse.from(Page.empty(pageable), this::toResponse);
        }
        Page<MenuItem> items = menuItemRepository
                .findByRestaurant_IdAndDeletedAtIsNullAndAvailableTrue(restaurantId, pageable);
        return PageResponse.from(items, this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getById(Long id) {
        return toResponse(findMenuItem(id));
    }

    @Override
    public MenuItemResponse update(Long id, UpdateMenuItemRequest request) {
        MenuItem item = findMenuItem(id);
        BigDecimal price = normalizePrice(request.getPrice());
        item.setName(requiredText(request.getName()));
        item.setDescription(optionalText(request.getDescription()));
        item.setPrice(price);
        return toResponse(menuItemRepository.save(item));
    }

    @Override
    public MenuItemResponse setAvailable(Long id, boolean available) {
        MenuItem item = findMenuItem(id);
        item.setAvailable(available);
        return toResponse(menuItemRepository.save(item));
    }

    @Override
    public void softDelete(Long id) {
        MenuItem item = findMenuItem(id);
        item.setDeletedAt(Instant.now());
        menuItemRepository.save(item);
    }

    private Restaurant findRestaurant(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> CatalogNotFoundException.restaurant(id));
    }

    private MenuItem findMenuItem(Long id) {
        return menuItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> CatalogNotFoundException.menuItem(id));
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new InvalidCatalogParameterException("price must be zero or greater");
        }
        if (price.scale() > 2) {
            throw new InvalidCatalogParameterException("price may have at most two decimal places");
        }
        try {
            BigDecimal normalized = price.setScale(2, RoundingMode.UNNECESSARY);
            if (normalized.precision() > 10) {
                throw new InvalidCatalogParameterException("price exceeds the supported maximum");
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw new InvalidCatalogParameterException("price may have at most two decimal places");
        }
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

    private MenuItemResponse toResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .restaurantId(item.getRestaurant().getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .currency(CURRENCY)
                .available(item.isAvailable())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
