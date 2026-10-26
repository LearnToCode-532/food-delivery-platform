package com.fooddelivery.catalog.restaurant.controller;

import com.fooddelivery.catalog.exception.CatalogExceptionHandler;
import com.fooddelivery.catalog.exception.CatalogNotFoundException;
import com.fooddelivery.catalog.exception.InvalidCatalogParameterException;
import com.fooddelivery.catalog.restaurant.dto.RestaurantResponse;
import com.fooddelivery.catalog.restaurant.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestaurantController.class)
@Import(CatalogExceptionHandler.class)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestaurantService restaurantService;

    @Test
    void createsRestaurantAndReturnsLocation() throws Exception {
        when(restaurantService.create(any())).thenReturn(restaurant());

        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType("application/json")
                        .content("""
                                {"name":"Example Kitchen","description":"Local food"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/restaurants/12"))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void invalidRestaurantBodyReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType("application/json")
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void invalidPageSizeReturnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants")
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidPageSortAndDirectionReturnBadRequest() throws Exception {
        when(restaurantService.list(0, 20, "unknown", "asc"))
                .thenThrow(new InvalidCatalogParameterException("Unsupported sort field"));
        when(restaurantService.list(0, 20, "name", "sideways"))
                .thenThrow(new InvalidCatalogParameterException("Invalid direction"));

        mockMvc.perform(get("/api/v1/restaurants")
                        .queryParam("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/v1/restaurants")
                        .queryParam("sortBy", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/v1/restaurants")
                        .queryParam("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidUpdateBodyReturnsSafeValidationResponse() throws Exception {
        mockMvc.perform(put("/api/v1/restaurants/12")
                        .contentType("application/json")
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void activationEndpointSetsExplicitState() throws Exception {
        when(restaurantService.setActive(12L, false)).thenReturn(restaurant(false));

        mockMvc.perform(patch("/api/v1/restaurants/12/activation")
                        .contentType("application/json")
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(restaurantService).setActive(12L, false);
    }

    @Test
    void restaurantNotFoundUsesConsistentJsonError() throws Exception {
        when(restaurantService.getById(999L)).thenThrow(CatalogNotFoundException.restaurant(999L));

        mockMvc.perform(get("/api/v1/restaurants/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/restaurants/999"));
    }

    @Test
    void forwardsBoundedPaginationParameters() throws Exception {
        when(restaurantService.list(eq(1), eq(5), eq("name"), eq("desc")))
                .thenReturn(com.fooddelivery.catalog.dto.PageResponse.<RestaurantResponse>builder()
                        .content(java.util.List.of(restaurant()))
                        .page(1)
                        .size(5)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/api/v1/restaurants")
                        .queryParam("page", "1")
                        .queryParam("size", "5")
                        .queryParam("sortBy", "name")
                        .queryParam("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content[0].name").value("Example Kitchen"));

        verify(restaurantService).list(1, 5, "name", "desc");
    }

    private RestaurantResponse restaurant() {
        return restaurant(true);
    }

    private RestaurantResponse restaurant(boolean active) {
        return RestaurantResponse.builder()
                .id(12L)
                .name("Example Kitchen")
                .active(active)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
