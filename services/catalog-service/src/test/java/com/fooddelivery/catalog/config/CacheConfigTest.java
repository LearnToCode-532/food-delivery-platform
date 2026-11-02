package com.fooddelivery.catalog.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class CacheConfigTest {

    @Test
    void catalogCacheUsesTenMinuteTtl() throws Exception {
        RedisCacheManager cacheManager =
                new CacheConfig().cacheManager(mock(RedisConnectionFactory.class));

        Cache cache = cacheManager.getCache("restaurants");
        Field targetCacheField = TransactionAwareCacheDecorator.class.getDeclaredField("targetCache");
        targetCacheField.setAccessible(true);
        Object targetCache = targetCacheField.get(cache);

        assertEquals(Duration.ofMinutes(10),
                targetCache.getClass()
                        .getMethod("getCacheConfiguration")
                        .invoke(targetCache)
                        .getClass()
                        .getMethod("getTtlFunction")
                        .invoke(targetCache.getClass().getMethod("getCacheConfiguration").invoke(targetCache))
                        .getClass()
                        .getMethod("getTimeToLive", Object.class, Object.class)
                        .invoke(
                                targetCache.getClass().getMethod("getCacheConfiguration").invoke(targetCache)
                                        .getClass().getMethod("getTtlFunction").invoke(
                                                targetCache.getClass().getMethod("getCacheConfiguration").invoke(targetCache)),
                                "key",
                                "value"));
    }
}
