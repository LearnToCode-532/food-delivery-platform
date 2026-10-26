package com.fooddelivery.catalog.menu.controller;

import com.fooddelivery.catalog.exception.CatalogExceptionHandler;
import com.fooddelivery.catalog.exception.CatalogNotFoundException;
import com.fooddelivery.catalog.exception.InvalidCatalogParameterException;
import com.fooddelivery.catalog.menu.dto.MenuItemResponse;
import com.fooddelivery.catalog.menu.service.MenuItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuItemController.class)
@Import(CatalogExceptionHandler.class)
class MenuItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuItemService menuItemService;

    @Test
    void createsMenuItemAndReturnsInrPrice() throws Exception {
        when(menuItemService.create(eq(7L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(menuItem());

        mockMvc.perform(post("/api/v1/restaurants/7/menu-items")
                        .contentType("application/json")
                        .content("""
                                {"name":"Soup","description":"Daily soup","price":12.50}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/menu-items/31"))
                .andExpect(jsonPath("$.price").value(12.5))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void rejectsNegativePriceInRequest() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants/7/menu-items")
                        .contentType("application/json")
                        .content("""
                                {"name":"Soup","price":-0.01}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void acceptsLargestSupportedPriceInRequest() throws Exception {
        when(menuItemService.create(eq(7L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(menuItem());

        mockMvc.perform(post("/api/v1/restaurants/7/menu-items")
                        .contentType("application/json")
                        .content("""
                                {"name":"Premium","price":99999999.99}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsExcessFractionalDigitsAndIntegerDigitsInRequest() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants/7/menu-items")
                        .contentType("application/json")
                        .content("""
                                {"name":"Soup","price":1.234}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.price").exists());

        mockMvc.perform(post("/api/v1/restaurants/7/menu-items")
                        .contentType("application/json")
                        .content("""
                                {"name":"Soup","price":1.230}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.price").exists());

        mockMvc.perform(post("/api/v1/restaurants/7/menu-items")
                        .contentType("application/json")
                        .content("""
                                {"name":"Soup","price":100000000.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void rejectsInvalidUpdateBodyWithSafeValidationResponse() throws Exception {
        mockMvc.perform(put("/api/v1/menu-items/31")
                        .contentType("application/json")
                        .content("""
                                {"name":" ","price":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void invalidMenuPaginationAndSortReturnSafeBadRequest() throws Exception {
        when(menuItemService.listForRestaurant(eq(7L), eq(0), eq(20), eq("unknown"), eq("asc")))
                .thenThrow(new InvalidCatalogParameterException("Unsupported sort field"));
        when(menuItemService.listForRestaurant(eq(7L), eq(0), eq(20), eq("name"), eq("sideways")))
                .thenThrow(new InvalidCatalogParameterException("direction must be 'asc' or 'desc'"));

        mockMvc.perform(get("/api/v1/restaurants/7/menu-items")
                        .queryParam("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/v1/restaurants/7/menu-items")
                        .queryParam("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/v1/restaurants/7/menu-items")
                        .queryParam("sortBy", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/v1/restaurants/7/menu-items")
                        .queryParam("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingRestaurantAndMenuItemUseSafeNotFoundResponses() throws Exception {
        when(menuItemService.listForRestaurant(999L, 0, 20, "name", "asc"))
                .thenThrow(CatalogNotFoundException.restaurant(999L));
        when(menuItemService.getById(999L)).thenThrow(CatalogNotFoundException.menuItem(999L));

        mockMvc.perform(get("/api/v1/restaurants/999/menu-items"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/v1/restaurants/999/menu-items"));

        mockMvc.perform(get("/api/v1/menu-items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/menu-items/999"));
    }

    @Test
    void marksMenuItemUnavailable() throws Exception {
        when(menuItemService.setAvailable(31L, false)).thenReturn(menuItem(false));

        mockMvc.perform(patch("/api/v1/menu-items/31/availability")
                        .contentType("application/json")
                        .content("""
                                {"available":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        verify(menuItemService).setAvailable(31L, false);
    }

    @Test
    void softDeleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/menu-items/31"))
                .andExpect(status().isNoContent());
        verify(menuItemService).softDelete(31L);
    }

    private MenuItemResponse menuItem() {
        return menuItem(true);
    }

    private MenuItemResponse menuItem(boolean available) {
        return MenuItemResponse.builder()
                .id(31L)
                .restaurantId(7L)
                .name("Soup")
                .description("Daily soup")
                .price(new java.math.BigDecimal("12.50"))
                .currency("INR")
                .available(available)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
