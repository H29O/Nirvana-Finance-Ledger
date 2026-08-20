package com.heet.backend.controller;

import com.heet.backend.dto.response.UserResponse;
import com.heet.backend.entity.User;
import com.heet.backend.security.UserPrincipal;
import com.heet.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserController userController;

    private UserPrincipal userPrincipal;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");

        userPrincipal = new UserPrincipal(user);

        userResponse = UserResponse.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();
    }

    @Test
    void getCurrentUser_Returns200OK() {
        when(authService.getCurrentUserProfile(any(UserPrincipal.class))).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = userController.getCurrentUser(userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John Doe", response.getBody().getName());
        assertEquals("john@example.com", response.getBody().getEmail());
    }
}
