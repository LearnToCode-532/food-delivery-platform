package com.fooddelivery.catalog.config;

import com.fooddelivery.catalog.menu.repository.MenuItemRepository;
import com.fooddelivery.catalog.menu.service.impl.MenuItemServiceImpl;
import com.fooddelivery.catalog.restaurant.repository.RestaurantRepository;
import com.fooddelivery.catalog.restaurant.service.impl.RestaurantServiceImpl;
import org.mockito.Mockito;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CatalogCachingTestConfig {

    @Bean
    public RestaurantRepository restaurantRepository() {
        return Mockito.mock(RestaurantRepository.class);
    }

    @Bean
    public MenuItemRepository menuItemRepository() {
        return Mockito.mock(MenuItemRepository.class);
    }

    @Bean
    public RestaurantServiceImpl restaurantService(RestaurantRepository restaurantRepository) {
        return new RestaurantServiceImpl(restaurantRepository);
    }

    @Bean
    public MenuItemServiceImpl menuItemService(
            MenuItemRepository menuItemRepository,
            RestaurantRepository restaurantRepository) {
        return new MenuItemServiceImpl(menuItemRepository, restaurantRepository);
    }

    @Bean
    public org.springframework.cache.CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("restaurants", "menu-items", "restaurant-menu-items");
    }
}
