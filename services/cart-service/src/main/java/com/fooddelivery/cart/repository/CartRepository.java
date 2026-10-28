package com.fooddelivery.cart.repository;

import com.fooddelivery.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findWithItemsByUserId(Long userId);

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findWithItemsByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
