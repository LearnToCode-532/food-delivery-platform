package com.fooddelivery.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cache.Cache;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class RedisCacheErrorHandlerTest {

    private final RedisCacheErrorHandler errorHandler = new RedisCacheErrorHandler();

    @Test
    void cacheReadFailureIsLoggedAndAllowsDatabaseFallback(CapturedOutput output) {
        Cache cache = cache();

        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(
                new DataAccessResourceFailureException("Redis unavailable"), cache, 42L));

        assertTrue(output.getOut().contains("Redis cache GET failed"));
        assertTrue(output.getOut().contains("Falling back to database"));
    }

    @Test
    void cacheWriteAndEvictionFailuresAreLoggedAndSuppressed(CapturedOutput output) {
        Cache cache = cache();
        DataAccessResourceFailureException failure =
                new DataAccessResourceFailureException("Redis unavailable");

        assertDoesNotThrow(() -> errorHandler.handleCachePutError(failure, cache, 42L, "value"));
        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(failure, cache, 42L));
        assertDoesNotThrow(() -> errorHandler.handleCacheClearError(failure, cache));

        assertTrue(output.getOut().contains("Redis cache PUT failed"));
        assertTrue(output.getOut().contains("Redis cache EVICT failed"));
        assertTrue(output.getOut().contains("Redis cache CLEAR failed"));
    }

    private Cache cache() {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("users");
        return cache;
    }
}
