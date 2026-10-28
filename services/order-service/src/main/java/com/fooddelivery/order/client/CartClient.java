package com.fooddelivery.order.client;

import com.fooddelivery.order.exception.CartNotFoundException;
import com.fooddelivery.order.exception.CartServiceUnavailableException;
import com.fooddelivery.order.exception.InvalidOrderRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CartClient {

    private final RestClient restClient;

    public CartClient(@Value("${cart.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public CartResponse getCart(Long userId, Long cartId) {
        try {
            return restClient.get()
                    .uri("/api/v1/users/{userId}/cart/{cartId}", userId, cartId)
                    .retrieve()
                    .body(CartResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new CartNotFoundException(userId, cartId);
            }
            if (exception.getStatusCode().is5xxServerError()) {
                throw new CartServiceUnavailableException();
            }
            throw new InvalidOrderRequestException("The cart request was rejected by Cart Service");
        } catch (ResourceAccessException exception) {
            throw new CartServiceUnavailableException();
        } catch (RestClientException exception) {
            throw new InvalidOrderRequestException("The cart request could not be processed");
        }
    }
}
