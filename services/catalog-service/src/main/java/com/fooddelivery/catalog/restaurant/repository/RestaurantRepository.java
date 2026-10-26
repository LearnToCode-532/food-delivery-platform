package com.fooddelivery.catalog.restaurant.repository;

import com.fooddelivery.catalog.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}
