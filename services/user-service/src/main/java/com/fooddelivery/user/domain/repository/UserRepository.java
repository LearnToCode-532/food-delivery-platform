package com.fooddelivery.user.domain.repository;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.domain.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndStatusNot(String email, UserStatus status);

    Optional<User> findByIdAndStatusNot(Long id, UserStatus status);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);
    
    Optional<User> findByPhoneNumber(String phoneNumber);
}
