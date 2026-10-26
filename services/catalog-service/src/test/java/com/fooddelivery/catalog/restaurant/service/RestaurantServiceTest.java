package com.fooddelivery.catalog.restaurant.service;

import com.fooddelivery.catalog.dto.PageResponse;
import com.fooddelivery.catalog.exception.InvalidCatalogParameterException;
import com.fooddelivery.catalog.exception.CatalogNotFoundException;
import com.fooddelivery.catalog.restaurant.dto.CreateRestaurantRequest;
import com.fooddelivery.catalog.restaurant.dto.RestaurantResponse;
import com.fooddelivery.catalog.restaurant.dto.UpdateRestaurantRequest;
import com.fooddelivery.catalog.restaurant.entity.Restaurant;
import com.fooddelivery.catalog.restaurant.repository.RestaurantRepository;
import com.fooddelivery.catalog.restaurant.service.impl.RestaurantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurant = Restaurant.builder()
                .id(12L)
                .name("Example Kitchen")
                .description("Local food")
                .address("Main Street")
                .phoneNumber("+911234567890")
                .active(true)
                .build();
    }

    @Test
    void createsRestaurantWithTrimmedFieldsAndActiveDefault() {
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });

        RestaurantResponse result = restaurantService.create(CreateRestaurantRequest.builder()
                .name("  Example Kitchen ")
                .description("  Local food  ")
                .address(" ")
                .phoneNumber("+911234567890")
                .build());

        assertEquals(12L, result.getId());
        assertEquals("Example Kitchen", result.getName());
        assertEquals("Local food", result.getDescription());
        assertEquals(null, result.getAddress());
        assertTrue(result.isActive());
    }

    @Test
    void retrievesExistingRestaurant() {
        when(restaurantRepository.findById(12L)).thenReturn(Optional.of(restaurant));

        assertEquals("Example Kitchen", restaurantService.getById(12L).getName());
    }

    @Test
    void missingRestaurantRaisesNotFound() {
        when(restaurantRepository.findById(12L)).thenReturn(Optional.empty());

        assertThrows(CatalogNotFoundException.class, () -> restaurantService.getById(12L));
    }

    @Test
    void updatesDetailsWithoutChangingActivation() {
        when(restaurantRepository.findById(12L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);

        RestaurantResponse result = restaurantService.update(12L, UpdateRestaurantRequest.builder()
                .name("  New Name ")
                .description(null)
                .address(" Updated Address ")
                .phoneNumber(null)
                .build());

        assertEquals("New Name", result.getName());
        assertEquals("Updated Address", result.getAddress());
        assertTrue(result.isActive());
    }

    @Test
    void activatesAndDeactivatesRestaurant() {
        when(restaurantRepository.findById(12L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);

        assertFalse(restaurantService.setActive(12L, false).isActive());
        assertTrue(restaurantService.setActive(12L, true).isActive());
    }

    @Test
    void listUsesRequestedPageAndStableIdTieBreaker() {
        when(restaurantRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(restaurant)));

        PageResponse<RestaurantResponse> result =
                restaurantService.list(2, 10, "name", "desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(restaurantRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals("name", pageable.getSort().toList().get(0).getProperty());
        assertEquals("id", pageable.getSort().toList().get(1).getProperty());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void listRejectsUnsupportedSortFields() {
        assertThrows(InvalidCatalogParameterException.class,
                () -> restaurantService.list(0, 20, "password", "asc"));
    }
}
