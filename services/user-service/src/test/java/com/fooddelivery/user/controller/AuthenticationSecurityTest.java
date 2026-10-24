package com.fooddelivery.user.controller;

import com.fooddelivery.user.auth.controller.AuthController;
import com.fooddelivery.user.auth.service.AuthService;
import com.fooddelivery.user.config.SecurityConfig;
import com.fooddelivery.user.domain.service.UserService;
import com.fooddelivery.user.dto.response.LoginResponse;
import com.fooddelivery.user.exception.GlobalExceptionHandler;
import com.fooddelivery.user.mapper.UserMapper;
import com.fooddelivery.user.security.JwtAuthenticationFilter;
import com.fooddelivery.user.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationFilter.class})
class AuthenticationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void loginWithInvalidCredentialsReturnsSafeUnauthorizedJson() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"john@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication failed"));
    }

    @Test
    void publicLoginStillReturnsSuccessfulAuthenticationResponse() throws Exception {
        when(authService.login(any())).thenReturn(LoginResponse.builder()
                .accessToken("test-access-token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"john@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void protectedEndpointWithoutJwtReturnsSafeUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/v1/users/42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void protectedEndpointWithInvalidJwtReturnsSafeUnauthorizedJson() throws Exception {
        when(jwtService.validateToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/42")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void accessDeniedExceptionReturnsSafeForbiddenJson() {
        HttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/users/42");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                new GlobalExceptionHandler().handleAccessDenied(new AccessDeniedException("Denied"), request);

        org.junit.jupiter.api.Assertions.assertEquals(403, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
        org.junit.jupiter.api.Assertions.assertEquals("Forbidden", response.getBody().getError());
        org.junit.jupiter.api.Assertions.assertEquals(
                "You are not authorized to access this resource",
                response.getBody().getMessage());
    }
}
