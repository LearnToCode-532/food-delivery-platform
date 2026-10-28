package com.fooddelivery.cart.controller;

import com.fooddelivery.cart.dto.CartItemResponse;
import com.fooddelivery.cart.dto.CartResponse;
import com.fooddelivery.cart.exception.CartExceptionHandler;
import com.fooddelivery.cart.exception.CartNotFoundException;
import com.fooddelivery.cart.exception.CartItemNotFoundException;
import com.fooddelivery.cart.exception.CatalogItemNotFoundException;
import com.fooddelivery.cart.exception.CatalogServiceUnavailableException;
import com.fooddelivery.cart.exception.InvalidCatalogResponseException;
import com.fooddelivery.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(CartExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void createsOrRetrievesAndGetsCart() throws Exception {
        when(cartService.createOrGet(5L)).thenReturn(cart());
        when(cartService.getByUserId(5L)).thenReturn(cart());

        mockMvc.perform(put("/api/v1/users/5/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.subtotal").value(0));
        mockMvc.perform(get("/api/v1/users/5/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void addsAndUpdatesItemWithQuantityAndNoClientPrice() throws Exception {
        when(cartService.addItem(5L, 11L, 31L, 2)).thenReturn(cartWithLine());
        when(cartService.updateQuantity(5L, 11L, 31L, 4)).thenReturn(cartWithLine());

        mockMvc.perform(post("/api/v1/users/5/cart/11/items/31")
                        .contentType("application/json")
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(12.5))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(put("/api/v1/users/5/cart/11/items/31")
                        .contentType("application/json")
                        .content("{\"quantity\":4}"))
                .andExpect(status().isOk());

        verify(cartService).addItem(5L, 11L, 31L, 2);
        verify(cartService).updateQuantity(5L, 11L, 31L, 4);
    }

    @Test
    void rejectsInvalidOrMalformedQuantityBodies() throws Exception {
        mockMvc.perform(post("/api/v1/users/5/cart/11/items/31")
                        .contentType("application/json")
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());

        mockMvc.perform(put("/api/v1/users/5/cart/11/items/31")
                        .contentType("application/json")
                        .content("{\"quantity\":\"not-a-number\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/users/5/cart/11/items/31")
                        .contentType("application/json")
                        .content("{\"quantity\":1.5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removesItemAndClearsCart() throws Exception {
        mockMvc.perform(delete("/api/v1/users/5/cart/11/items/31"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/users/5/cart/11/items"))
                .andExpect(status().isNoContent());
        verify(cartService).removeItem(5L, 11L, 31L);
        verify(cartService).clear(5L, 11L);
    }

    @Test
    void mapsWrongOwnerMissingCartAndCatalogFailuresToConsistentErrors() throws Exception {
        when(cartService.getByIdAndUserId(11L, 99L)).thenThrow(new CartNotFoundException());
        when(cartService.addItem(5L, 11L, 31L, 1))
                .thenThrow(new CatalogServiceUnavailableException());
        when(cartService.addItem(5L, 11L, 32L, 1))
                .thenThrow(new InvalidCatalogResponseException());
        when(cartService.addItem(5L, 11L, 34L, 1))
                .thenThrow(new CatalogItemNotFoundException(34L));
        when(cartService.updateQuantity(5L, 11L, 33L, 1))
                .thenThrow(new CartItemNotFoundException(33L));

        mockMvc.perform(get("/api/v1/users/99/cart/11"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/v1/users/99/cart/11"));

        mockMvc.perform(post("/api/v1/users/5/cart/11/items/31")
                        .contentType("application/json")
                        .content("{\"quantity\":1}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        mockMvc.perform(post("/api/v1/users/5/cart/11/items/32")
                        .contentType("application/json")
                        .content("{\"quantity\":1}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));

        mockMvc.perform(put("/api/v1/users/5/cart/11/items/33")
                        .contentType("application/json")
                        .content("{\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Menu item 33 is not in this cart"));

        mockMvc.perform(post("/api/v1/users/5/cart/11/items/34")
                        .contentType("application/json")
                        .content("{\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Menu item 34 was not found in the Catalog Service"));
    }

    @Test
    void mapsPersistenceConstraintFailureToConflict() throws Exception {
        when(cartService.createOrGet(5L)).thenThrow(new DataIntegrityViolationException("constraint"));

        mockMvc.perform(put("/api/v1/users/5/cart"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void rejectsNonPositiveUserCartAndMenuItemIds() throws Exception {
        mockMvc.perform(get("/api/v1/users/0/cart"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/users/5/cart/0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/v1/users/5/cart/11/items/0"))
                .andExpect(status().isBadRequest());
    }

    private CartResponse cart() {
        return new CartResponse(11L, 5L, List.of(), new BigDecimal("0.00"), "INR",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private CartResponse cartWithLine() {
        CartItemResponse item = new CartItemResponse(
                31L, 31L, "Soup", 2, new BigDecimal("12.50"),
                new BigDecimal("25.00"), "INR");
        return new CartResponse(11L, 5L, List.of(item), new BigDecimal("25.00"), "INR",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
