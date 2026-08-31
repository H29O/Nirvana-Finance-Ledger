package com.heet.backend.service;

import com.heet.backend.dto.request.LoginRequest;
import com.heet.backend.dto.request.RegisterRequest;
import com.heet.backend.dto.response.AuthResponse;
import com.heet.backend.dto.response.UserResponse;
import com.heet.backend.entity.User;
import com.heet.backend.exception.DuplicateEmailException;
import com.heet.backend.exception.InvalidCredentialsException;
import com.heet.backend.repository.UserRepository;
import com.heet.backend.security.JwtTokenProvider;
import com.heet.backend.security.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CategoryService categoryService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            CategoryService categoryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.categoryService = categoryService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String trimmedEmail = request.getEmail().trim();

        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new DuplicateEmailException(trimmedEmail);
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(trimmedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        // Seed initial default categories for the new user account
        categoryService.createDefaultCategoriesForUser(savedUser);

        String token = jwtTokenProvider.generateToken(savedUser.getEmail());
        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(savedUser))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String trimmedEmail = request.getEmail().trim();

        User user = userRepository.findByEmail(trimmedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new InvalidCredentialsException());
        return mapToUserResponse(user);
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
