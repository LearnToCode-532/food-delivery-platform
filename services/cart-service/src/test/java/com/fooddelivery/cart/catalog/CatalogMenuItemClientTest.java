package com.fooddelivery.cart.catalog;

import com.fooddelivery.cart.exception.CatalogItemNotFoundException;
import com.fooddelivery.cart.exception.CatalogServiceUnavailableException;
import com.fooddelivery.cart.exception.InvalidCatalogResponseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogMenuItemClientTest {

    private MockRestServiceServer server;
    private CatalogMenuItemClient catalogClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://catalog.test");
        server = MockRestServiceServer.bindTo(builder).build();
        catalogClient = new CatalogMenuItemClient(builder.build());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void retrievesCurrentMenuItemDetailsFromCatalogApi() {
        server.expect(requestTo("http://catalog.test/api/v1/menu-items/31"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":31,"name":"Soup","price":12.50,"currency":"INR","available":true}
                        """, MediaType.APPLICATION_JSON));

        CatalogMenuItem item = catalogClient.getMenuItem(31L);

        assertEquals(31L, item.id());
        assertEquals("Soup", item.name());
        assertEquals("12.50", item.price().toPlainString());
    }

    @Test
    void mapsMissingCatalogItemToNotFound() {
        server.expect(requestTo("http://catalog.test/api/v1/menu-items/31"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThrows(CatalogItemNotFoundException.class, () -> catalogClient.getMenuItem(31L));
    }

    @Test
    void mapsCatalogOutageToUnavailable() {
        server.expect(requestTo("http://catalog.test/api/v1/menu-items/32"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThrows(CatalogServiceUnavailableException.class, () -> catalogClient.getMenuItem(32L));
    }

    @Test
    void mapsCatalogConnectionFailureToUnavailable() {
        server.expect(requestTo("http://catalog.test/api/v1/menu-items/32"))
                .andRespond(withException(new IOException("connection failed")));
        assertThrows(CatalogServiceUnavailableException.class, () -> catalogClient.getMenuItem(32L));
    }

    @Test
    void rejectsMalformedCatalogResponse() {
        server.expect(requestTo("http://catalog.test/api/v1/menu-items/31"))
                .andRespond(withSuccess("{not-json", MediaType.APPLICATION_JSON));
        assertThrows(InvalidCatalogResponseException.class, () -> catalogClient.getMenuItem(31L));
    }

    @Test
    void rejectsMismatchedIdAndInvalidCurrency() {
        server.expect(requestTo("http://catalog.test/api/v1/menu-items/32"))
                .andRespond(withSuccess("""
                        {"id":99,"name":"Soup","price":12.50,"currency":"INR","available":true}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://catalog.test/api/v1/menu-items/33"))
                .andRespond(withSuccess("""
                        {"id":33,"name":"Soup","price":12.50,"currency":"USD","available":true}
                        """, MediaType.APPLICATION_JSON));
        assertThrows(InvalidCatalogResponseException.class, () -> catalogClient.getMenuItem(32L));
        assertThrows(InvalidCatalogResponseException.class, () -> catalogClient.getMenuItem(33L));
    }

    @Test
    void rejectsMissingAvailabilityOrPriceDetails() {
        server.expect(requestTo("http://catalog.test/api/v1/menu-items/31"))
                .andRespond(withSuccess("""
                        {"id":31,"name":"Soup","price":12.50,"currency":"INR"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://catalog.test/api/v1/menu-items/32"))
                .andRespond(withSuccess("""
                        {"id":32,"name":"Soup","currency":"INR","available":true}
                        """, MediaType.APPLICATION_JSON));
        assertThrows(InvalidCatalogResponseException.class, () -> catalogClient.getMenuItem(31L));
        assertThrows(InvalidCatalogResponseException.class, () -> catalogClient.getMenuItem(32L));
    }
}
