package com.fooddelivery.catalog.menu.service;

import com.fooddelivery.catalog.dto.PageResponse;
import com.fooddelivery.catalog.exception.CatalogNotFoundException;
import com.fooddelivery.catalog.exception.InvalidCatalogParameterException;
import com.fooddelivery.catalog.menu.dto.CreateMenuItemRequest;
import com.fooddelivery.catalog.menu.dto.MenuItemResponse;
import com.fooddelivery.catalog.menu.dto.UpdateMenuItemRequest;
import com.fooddelivery.catalog.menu.entity.MenuItem;
import com.fooddelivery.catalog.menu.repository.MenuItemRepository;
import com.fooddelivery.catalog.menu.service.impl.MenuItemServiceImpl;
import com.fooddelivery.catalog.restaurant.entity.Restaurant;
import com.fooddelivery.catalog.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private MenuItemServiceImpl menuItemService;

    private Restaurant restaurant;
    private MenuItem item;

    @BeforeEach
    void setUp() {
        restaurant = Restaurant.builder().id(7L).name("Example Kitchen").active(true).build();
        item = MenuItem.builder()
                .id(31L)
                .restaurant(restaurant)
                .name("Soup")
                .description("Daily soup")
                .price(new BigDecimal("12.50"))
                .available(true)
                .build();
    }

    @Test
    void createsMenuItemForExistingRestaurantWithDefaultAvailability() {
        when(restaurantRepository.findById(7L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem saved = invocation.getArgument(0);
            saved.setId(31L);
            return saved;
        });

        MenuItemResponse result = menuItemService.create(7L, CreateMenuItemRequest.builder()
                .name(" Soup ")
                .description("  Daily soup ")
                .price(new BigDecimal("12.5"))
                .build());

        assertEquals(31L, result.getId());
        assertEquals(7L, result.getRestaurantId());
        assertEquals("Soup", result.getName());
        assertEquals(new BigDecimal("12.50"), result.getPrice());
        assertEquals("INR", result.getCurrency());
        assertTrue(result.isAvailable());
    }

    @Test
    void missingRestaurantPreventsCreation() {
        when(restaurantRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(CatalogNotFoundException.class, () -> menuItemService.create(
                7L,
                CreateMenuItemRequest.builder().name("Soup").price(BigDecimal.ONE).build()));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void getsNonDeletedMenuItemIncludingItsAvailabilityState() {
        item.setAvailable(false);
        when(menuItemRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.of(item));

        MenuItemResponse result = menuItemService.getById(31L);

        assertEquals(7L, result.getRestaurantId());
        assertFalse(result.isAvailable());
    }

    @Test
    void missingOrDeletedMenuItemReturnsNotFound() {
        when(menuItemRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.empty());

        assertThrows(CatalogNotFoundException.class, () -> menuItemService.getById(31L));
    }

    @Test
    void softDeletedMenuItemIsExcludedFromGetAndRestaurantMenuList() {
        item.setDeletedAt(Instant.now());
        when(menuItemRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.empty());
        when(restaurantRepository.findById(7L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurant_IdAndDeletedAtIsNullAndAvailableTrue(
                org.mockito.ArgumentMatchers.eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThrows(CatalogNotFoundException.class, () -> menuItemService.getById(31L));
        PageResponse<MenuItemResponse> result =
                menuItemService.listForRestaurant(7L, 0, 20, "name", "asc");

        assertTrue(result.getContent().isEmpty());
        verify(menuItemRepository).findByRestaurant_IdAndDeletedAtIsNullAndAvailableTrue(
                org.mockito.ArgumentMatchers.eq(7L), any(Pageable.class));
    }

    @Test
    void updatesItemDetailsAndPriceWithoutChangingRestaurant() {
        when(menuItemRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        MenuItemResponse result = menuItemService.update(31L, UpdateMenuItemRequest.builder()
                .name("  Updated soup ")
                .description(null)
                .price(new BigDecimal("25.00"))
                .build());

        assertEquals("Updated soup", result.getName());
        assertEquals(new BigDecimal("25.00"), result.getPrice());
        assertEquals(7L, result.getRestaurantId());
    }

    @Test
    void rejectsNegativePriceEvenWhenCalledOutsideControllerValidation() {
        assertThrows(InvalidCatalogParameterException.class, () -> menuItemService.create(
                7L,
                CreateMenuItemRequest.builder().name("Soup").price(new BigDecimal("-0.01")).build()));
        verify(restaurantRepository, never()).findById(anyLong());
    }

    @Test
    void acceptsLargestSupportedPrice() {
        when(restaurantRepository.findById(7L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse result = menuItemService.create(7L, CreateMenuItemRequest.builder()
                .name("Premium")
                .price(new BigDecimal("99999999.99"))
                .build());

        assertEquals(new BigDecimal("99999999.99"), result.getPrice());
    }

    @Test
    void rejectsMoreThanTwoFractionalDigits() {
        assertThrows(InvalidCatalogParameterException.class, () -> menuItemService.create(
                7L,
                CreateMenuItemRequest.builder().name("Soup").price(new BigDecimal("1.234")).build()));
        assertThrows(InvalidCatalogParameterException.class, () -> menuItemService.create(
                7L,
                CreateMenuItemRequest.builder().name("Soup").price(new BigDecimal("1.230")).build()));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void rejectsPriceAboveSupportedIntegerDigits() {
        assertThrows(InvalidCatalogParameterException.class, () -> menuItemService.create(
                7L,
                CreateMenuItemRequest.builder().name("Soup").price(new BigDecimal("100000000.00")).build()));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void invalidUpdatePriceDoesNotMutateOrSaveManagedItem() {
        when(menuItemRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.of(item));

        assertThrows(InvalidCatalogParameterException.class, () -> menuItemService.update(
                31L,
                UpdateMenuItemRequest.builder()
                        .name("Changed")
                        .description("Changed description")
                        .price(new BigDecimal("1.234"))
                        .build()));

        assertEquals("Soup", item.getName());
        assertEquals("Daily soup", item.getDescription());
        assertEquals(new BigDecimal("12.50"), item.getPrice());
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void changesAvailabilityForNonDeletedItem() {
        when(menuItemRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        assertFalse(menuItemService.setAvailable(31L, false).isAvailable());
    }

    @Test
    void softDeletesItemAndRepeatedDeleteIsNotFound() {
        when(menuItemRepository.findByIdAndDeletedAtIsNull(31L))
                .thenReturn(Optional.of(item), Optional.empty());
        when(menuItemRepository.save(item)).thenReturn(item);

        menuItemService.softDelete(31L);

        assertNotNull(item.getDeletedAt());
        verify(menuItemRepository).save(item);
        assertThrows(CatalogNotFoundException.class, () -> menuItemService.softDelete(31L));
    }

    @Test
    void updatingSoftDeletedItemReturnsNotFound() {
        item.setDeletedAt(Instant.now());
        when(menuItemRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.empty());

        assertThrows(CatalogNotFoundException.class, () -> menuItemService.update(
                31L,
                UpdateMenuItemRequest.builder()
                        .name("Restored")
                        .price(new BigDecimal("12.50"))
                        .build()));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void changingAvailabilityOfSoftDeletedItemReturnsNotFound() {
        item.setDeletedAt(Instant.now());
        when(menuItemRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.empty());

        assertThrows(CatalogNotFoundException.class, () -> menuItemService.setAvailable(31L, true));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void listFiltersUnavailableAndDeletedItemsAndHidesInactiveRestaurantMenu() {
        when(restaurantRepository.findById(7L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurant_IdAndDeletedAtIsNullAndAvailableTrue(
                org.mockito.ArgumentMatchers.eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        PageResponse<MenuItemResponse> result =
                menuItemService.listForRestaurant(7L, 0, 20, "name", "asc");
        assertEquals(1, result.getContent().size());

        restaurant.setActive(false);
        PageResponse<MenuItemResponse> hidden =
                menuItemService.listForRestaurant(7L, 0, 20, "name", "asc");
        assertTrue(hidden.getContent().isEmpty());
        verify(menuItemRepository).findByRestaurant_IdAndDeletedAtIsNullAndAvailableTrue(
                org.mockito.ArgumentMatchers.eq(7L), any(Pageable.class));
    }
}
