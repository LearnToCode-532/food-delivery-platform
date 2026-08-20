package com.fooddelivery.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter.
 * 
 * Executes once per request to:
 * 1. Read the Authorization header
 * 2. Verify that the scheme is Bearer
 * 3. Extract the JWT
 * 4. Validate the JWT using JwtService
 * 5. Extract the user identity from the JWT
 * 6. Create an Authentication object
 * 7. Store it in SecurityContextHolder
 * 8. Continue the filter chain
 * 
 * Cases handled:
 * - No Authorization header: Continue filter chain (SecurityFilterChain determines access)
 * - Non-Bearer Authorization: Continue without authenticating
 * - Malformed JWT: Do not authenticate, request fails if endpoint requires auth
 * - Expired JWT: Do not authenticate, return 401 for protected endpoints
 * - Invalid signature: Do not authenticate, return 401 for protected endpoints
 * - Valid JWT: Create authenticated SecurityContext and continue
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null && jwtService.validateToken(jwt)) {
                String email = jwtService.getSubjectFromToken(jwt);
                
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Set Spring Security authentication for user: {}", email);
            }
        } catch (Exception e) {

            // Invalid/expired/malformed JWT.
            SecurityContextHolder.clearContext();
            log.debug("Could not set user authentication in security context: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT from Authorization header.
     * 
     * Expected format: Authorization: Bearer <JWT>
     * 
     * @param request HttpServletRequest
     * @return JWT token, or null if not found or not Bearer scheme
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}
