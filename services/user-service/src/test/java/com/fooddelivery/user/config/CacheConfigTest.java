package com.fooddelivery.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class CacheConfigTest {

    @Test
    void userCacheKeepsTenMinuteTtlAndTransactionAwareEviction() {
        RedisCacheManager cacheManager =
                new CacheConfig().cacheManager(mock(RedisConnectionFactory.class));
        TransactionAwareCacheDecorator cache =
                assertInstanceOf(TransactionAwareCacheDecorator.class, cacheManager.getCache("users"));
        RedisCache redisCache = assertInstanceOf(RedisCache.class, cache.getTargetCache());

        assertEquals(Duration.ofMinutes(10),
                redisCache.getCacheConfiguration()
                        .getTtlFunction().getTimeToLive("key", "value"));
    }
}
