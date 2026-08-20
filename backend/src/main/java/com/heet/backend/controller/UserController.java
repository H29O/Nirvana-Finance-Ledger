package com.heet.backend.controller;

import com.heet.backend.dto.response.UserResponse;
import com.heet.backend.security.UserPrincipal;
import com.heet.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse userResponse = authService.getCurrentUserProfile(principal);
        return ResponseEntity.ok(userResponse);
    }
}
