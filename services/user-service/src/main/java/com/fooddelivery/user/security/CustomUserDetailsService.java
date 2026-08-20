package com.fooddelivery.user.security;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.domain.enums.UserStatus;
import com.fooddelivery.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Custom UserDetailsService implementation.
 * 
 * Uses email as the authentication username.
 * Only ACTIVE users can authenticate.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        // Only ACTIVE users can authenticate
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new UsernameNotFoundException("User account is not active");
        }
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                isAccountNonLocked(user.getStatus()),
                true, // accountNonExpired
                true, // credentialsNonExpired
                isEnabled(user.getStatus()),
                getAuthorities()
        );
    }
    
    /**
     * Returns authorities for the user.
     * Currently returns empty list as RBAC is not part of Phase 2.
     */
    private Collection<? extends GrantedAuthority> getAuthorities() {
        return new ArrayList<>();
    }
    
    /**
     * Account is locked if user status is SUSPENDED.
     */
    private boolean isAccountNonLocked(UserStatus status) {
        return !UserStatus.SUSPENDED.equals(status);
    }
    
    /**
     * Account is enabled if user status is ACTIVE.
     */
    private boolean isEnabled(UserStatus status) {
        return UserStatus.ACTIVE.equals(status);
    }
}
