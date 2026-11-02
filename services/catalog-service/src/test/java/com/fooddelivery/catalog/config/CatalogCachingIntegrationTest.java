package com.fooddelivery.catalog.config;

import com.fooddelivery.catalog.exception.CatalogNotFoundException;
import com.fooddelivery.catalog.menu.dto.UpdateMenuItemRequest;
import com.fooddelivery.catalog.menu.entity.MenuItem;
import com.fooddelivery.catalog.menu.repository.MenuItemRepository;
import com.fooddelivery.catalog.menu.service.MenuItemService;
import com.fooddelivery.catalog.restaurant.dto.UpdateRestaurantRequest;
import com.fooddelivery.catalog.restaurant.entity.Restaurant;
import com.fooddelivery.catalog.restaurant.repository.RestaurantRepository;
import com.fooddelivery.catalog.restaurant.service.RestaurantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogCachingIntegrationTest {

    private AnnotationConfigApplicationContext context;
    private RestaurantService restaurantService;
    private MenuItemService menuItemService;
    private RestaurantRepository restaurantRepository;
    private MenuItemRepository menuItemRepository;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(CatalogCachingTestConfig.class);
        context.refresh();
        restaurantService = context.getBean(RestaurantService.class);
        menuItemService = context.getBean(MenuItemService.class);
        restaurantRepository = context.getBean(RestaurantRepository.class);
        menuItemRepository = context.getBean(MenuItemRepository.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void restaurantLookupUsesCacheAndMissesOnlyOnce() {
        Restaurant restaurant = Restaurant.builder()
                .id(42L)
                .name("Bistro")
                .description("Open late")
                .address("Central")
                .phoneNumber("123")
                .active(true)
                .build();
        when(restaurantRepository.findById(42L)).thenReturn(Optional.of(restaurant));

        assertEquals("Bistro", restaurantService.getById(42L).getName());
        assertEquals("Bistro", restaurantService.getById(42L).getName());

        verify(restaurantRepository, times(1)).findById(42L);
    }

    @Test
    void restaurantUpdateInvalidatesCachedRestaurantDetail() {
        Restaurant original = Restaurant.builder()
                .id(42L)
                .name("Old name")
                .description("Old description")
                .address("Old address")
                .phoneNumber("111")
                .active(true)
                .build();
        Restaurant updated = Restaurant.builder()
                .id(42L)
                .name("New name")
                .description("Updated")
                .address("New address")
                .phoneNumber("222")
                .active(true)
                .build();
        when(restaurantRepository.findById(42L)).thenReturn(Optional.of(original), Optional.of(updated), Optional.of(updated));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        restaurantService.getById(42L);
        restaurantService.update(42L, UpdateRestaurantRequest.builder()
                .name("New name")
                .description("Updated")
                .address("New address")
                .phoneNumber("222")
                .build());
        assertEquals("New name", restaurantService.getById(42L).getName());

        verify(restaurantRepository, times(3)).findById(42L);
    }

    @Test
    void menuItemLookupUsesCacheAndInvalidatesAfterAvailabilityChange() {
        Restaurant restaurant = Restaurant.builder()
                .id(5L)
                .name("Pizza House")
                .description("Italian")
                .address("Main")
                .phoneNumber("555")
                .active(true)
                .build();
        MenuItem availableItem = MenuItem.builder()
                .id(11L)
                .restaurant(restaurant)
                .name("Margherita")
                .description("Classic")
                .price(new BigDecimal("9.99"))
                .available(true)
                .build();
        MenuItem unavailableItem = MenuItem.builder()
                .id(11L)
                .restaurant(restaurant)
                .name("Margherita")
                .description("Classic")
                .price(new BigDecimal("9.99"))
                .available(false)
                .build();

        when(menuItemRepository.findByIdAndDeletedAtIsNull(11L))
                .thenReturn(Optional.of(availableItem), Optional.of(unavailableItem), Optional.of(unavailableItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("Margherita", menuItemService.getById(11L).getName());
        assertEquals("Margherita", menuItemService.getById(11L).getName());
        verify(menuItemRepository, times(1)).findByIdAndDeletedAtIsNull(11L);

        menuItemService.setAvailable(11L, false);
        assertEquals(false, menuItemService.getById(11L).isAvailable());
        verify(menuItemRepository, times(3)).findByIdAndDeletedAtIsNull(11L);
    }

    @Test
    void menuItemUpdateInvalidatesCachedDetail() {
        Restaurant restaurant = Restaurant.builder()
                .id(5L)
                .name("Pizza House")
                .description("Italian")
                .address("Main")
                .phoneNumber("555")
                .active(true)
                .build();
        MenuItem original = MenuItem.builder()
                .id(11L)
                .restaurant(restaurant)
                .name("Margherita")
                .description("Classic")
                .price(new BigDecimal("9.99"))
                .available(true)
                .build();
        MenuItem updated = MenuItem.builder()
                .id(11L)
                .restaurant(restaurant)
                .name("Pepperoni")
                .description("Spicy")
                .price(new BigDecimal("12.50"))
                .available(true)
                .build();

        when(menuItemRepository.findByIdAndDeletedAtIsNull(11L))
                .thenReturn(Optional.of(original), Optional.of(updated), Optional.of(updated));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("Margherita", menuItemService.getById(11L).getName());
        menuItemService.update(11L, UpdateMenuItemRequest.builder()
                .name("Pepperoni")
                .description("Spicy")
                .price(new BigDecimal("12.50"))
                .build());
        assertEquals("Pepperoni", menuItemService.getById(11L).getName());

        verify(menuItemRepository, times(3)).findByIdAndDeletedAtIsNull(11L);
    }

    @Test
    void softDeleteRemovesCachedItem() {
        Restaurant restaurant = Restaurant.builder()
                .id(5L)
                .name("Pizza House")
                .description("Italian")
                .address("Main")
                .phoneNumber("555")
                .active(true)
                .build();
        MenuItem item = MenuItem.builder()
                .id(11L)
                .restaurant(restaurant)
                .name("Margherita")
                .description("Classic")
                .price(new BigDecimal("9.99"))
                .available(true)
                .build();

        when(menuItemRepository.findByIdAndDeletedAtIsNull(11L))
                .thenReturn(Optional.of(item), Optional.of(item), Optional.empty());
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("Margherita", menuItemService.getById(11L).getName());
        menuItemService.softDelete(11L);

        assertThrows(CatalogNotFoundException.class, () -> menuItemService.getById(11L));
        verify(menuItemRepository, times(3)).findByIdAndDeletedAtIsNull(11L);
    }

    @Test
    void restaurantMenuListIsCachedAndInvalidatedOnMutation() {
        Restaurant restaurant = Restaurant.builder()
                .id(5L)
                .name("Pizza House")
                .description("Italian")
                .address("Main")
                .phoneNumber("555")
                .active(true)
                .build();
        MenuItem original = MenuItem.builder()
                .id(11L)
                .restaurant(restaurant)
                .name("Margherita")
                .description("Classic")
                .price(new BigDecimal("9.99"))
                .available(true)
                .build();
        MenuItem updated = MenuItem.builder()
                .id(11L)
                .restaurant(restaurant)
                .name("Pepperoni")
                .description("Spicy")
                .price(new BigDecimal("12.50"))
                .available(true)
                .build();

        Page<MenuItem> firstPage = new PageImpl<>(List.of(original), PageRequest.of(0, 10), 1);
        Page<MenuItem> secondPage = new PageImpl<>(List.of(updated), PageRequest.of(0, 10), 1);

        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurant_IdAndDeletedAtIsNullAndAvailableTrue(eq(5L), any(Pageable.class)))
                .thenReturn(firstPage, secondPage);
        when(menuItemRepository.findByIdAndDeletedAtIsNull(11L))
                .thenReturn(Optional.of(original), Optional.of(updated), Optional.of(updated));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("Margherita", menuItemService.listForRestaurant(5L, 0, 10, "id", "ASC").getContent().get(0).getName());
        assertEquals("Margherita", menuItemService.listForRestaurant(5L, 0, 10, "id", "ASC").getContent().get(0).getName());
        verify(menuItemRepository, times(1)).findByRestaurant_IdAndDeletedAtIsNullAndAvailableTrue(eq(5L), any(Pageable.class));

        menuItemService.update(11L, UpdateMenuItemRequest.builder()
                .name("Pepperoni")
                .description("Spicy")
                .price(new BigDecimal("12.50"))
                .build());
        assertEquals("Pepperoni", menuItemService.listForRestaurant(5L, 0, 10, "id", "ASC").getContent().get(0).getName());
        verify(menuItemRepository, times(2)).findByRestaurant_IdAndDeletedAtIsNullAndAvailableTrue(eq(5L), any(Pageable.class));
    }
}
