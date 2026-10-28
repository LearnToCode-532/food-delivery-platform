package com.fooddelivery.cart.catalog;

import com.fooddelivery.cart.exception.CatalogItemNotFoundException;
import com.fooddelivery.cart.exception.CatalogServiceUnavailableException;
import com.fooddelivery.cart.exception.InvalidCatalogResponseException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.RoundingMode;

@Component
public class CatalogMenuItemClient {

    private final RestClient restClient;

    public CatalogMenuItemClient(RestClient catalogRestClient) {
        this.restClient = catalogRestClient;
    }

    public CatalogMenuItem getMenuItem(long menuItemId) {
        CatalogMenuItem item;
        try {
            item = restClient.get()
                    .uri("/api/v1/menu-items/{id}", menuItemId)
                    .retrieve()
                    .body(CatalogMenuItem.class);
        } catch (RestClientResponseException exception) {
            HttpStatusCode status = exception.getStatusCode();
            if (status.value() == 404) {
                throw new CatalogItemNotFoundException(menuItemId);
            }
            if (status.is5xxServerError()) {
                throw new CatalogServiceUnavailableException();
            }
            throw new InvalidCatalogResponseException();
        } catch (ResourceAccessException exception) {
            throw new CatalogServiceUnavailableException();
        } catch (RestClientException exception) {
            throw new InvalidCatalogResponseException();
        }

        validateResponse(menuItemId, item);
        return item;
    }

    private void validateResponse(long requestedId, CatalogMenuItem item) {
        if (item == null
                || item.id() == null
                || item.id() != requestedId
                || item.name() == null
                || item.name().isBlank()
                || item.price() == null
                || item.price().signum() < 0
                || item.currency() == null
                || !"INR".equals(item.currency())
                || item.available() == null) {
            throw new InvalidCatalogResponseException();
        }

        try {
            if (item.price().setScale(2, RoundingMode.UNNECESSARY).precision() > 10) {
                throw new InvalidCatalogResponseException();
            }
        } catch (ArithmeticException exception) {
            throw new InvalidCatalogResponseException();
        }
    }
}
