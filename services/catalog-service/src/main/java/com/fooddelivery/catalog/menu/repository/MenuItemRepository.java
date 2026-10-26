package com.fooddelivery.catalog.menu.repository;

import com.fooddelivery.catalog.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    Optional<MenuItem> findByIdAndDeletedAtIsNull(Long id);

    Page<MenuItem> findByRestaurant_IdAndDeletedAtIsNullAndAvailableTrue(
            Long restaurantId,
            Pageable pageable);
}
