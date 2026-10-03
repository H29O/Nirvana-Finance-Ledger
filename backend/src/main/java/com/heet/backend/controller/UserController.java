package com.heet.backend.controller;

import com.heet.backend.dto.response.UserResponse;
import com.heet.backend.security.UserPrincipal;
import com.heet.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse userResponse = userService.getCurrentUserProfile(principal);
        return ResponseEntity.ok(userResponse);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Object>> deleteCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteAccount(principal);
        return ResponseEntity.ok(Map.of(
                "message", "Your account has been permanently deleted.",
                "success", true
        ));
    }
}
