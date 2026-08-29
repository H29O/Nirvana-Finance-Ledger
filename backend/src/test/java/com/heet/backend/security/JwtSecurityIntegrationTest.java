package com.heet.backend.security;

import com.heet.backend.entity.User;
import com.heet.backend.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class JwtSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private MockMvc mockMvc;
    private User testUser1;
    private User testUser2;
    private String validTokenUser1;
    private String validTokenUser2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        testUser1 = new User();
        testUser1.setName("Security Test User 1");
        testUser1.setEmail("secuser1@example.com");
        testUser1.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setName("Security Test User 2");
        testUser2.setEmail("secuser2@example.com");
        testUser2.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser2 = userRepository.save(testUser2);

        validTokenUser1 = jwtTokenProvider.generateToken(testUser1.getEmail());
        validTokenUser2 = jwtTokenProvider.generateToken(testUser2.getEmail());
    }

    @Test
    void protectedEndpoints_WithoutAuthentication_Return401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        mockMvc.perform(get("/api/wrapped"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        mockMvc.perform(get("/api/receipts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        mockMvc.perform(get("/api/analytics/overview")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void protectedEndpoint_WithMalformedOrInvalidJwt_Return401Unauthorized() throws Exception {
        // Random malformed token
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalid.malformed.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        // Empty token after Bearer prefix
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        // Non-Bearer authorization header prefix
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void protectedEndpoint_WithExpiredJwt_Return401Unauthorized() throws Exception {
        // Generate an expired token signed with the valid secret key
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date pastExpiry = new Date(now.getTime() - 60000); // 1 minute in the past

        String expiredToken = Jwts.builder()
                .subject(testUser1.getEmail())
                .issuedAt(new Date(now.getTime() - 120000))
                .expiration(pastExpiry)
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void publicAuthEndpoints_AccessibleWithoutJwt() throws Exception {
        // Validation error on register is reached (400 Bad Request, not rejected as 401 by security filter)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // Login endpoint is reached (returns 400 Bad Request on empty payload, not 401 filter rejection)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validJwt_SuccessfullyAuthenticatesAndReturnsUserData() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + validTokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser1.getId().intValue())))
                .andExpect(jsonPath("$.email", is("secuser1@example.com")))
                .andExpect(jsonPath("$.name", is("Security Test User 1")));
    }

    @Test
    void userIdentity_CorrectlyResolvedPerToken() throws Exception {
        // User 1 token resolves to User 1
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + validTokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser1.getId().intValue())))
                .andExpect(jsonPath("$.email", is("secuser1@example.com")));

        // User 2 token resolves to User 2
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + validTokenUser2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser2.getId().intValue())))
                .andExpect(jsonPath("$.email", is("secuser2@example.com")));
    }

    @Test
    void userIdentity_CannotBeForgedByQueryParamOrHeader() throws Exception {
        // Authenticated as User 1, but attacker sends query param ?userId=User2.id and header X-User-Id=User2.id
        mockMvc.perform(get("/api/users/me")
                        .param("userId", testUser2.getId().toString())
                        .header("X-User-Id", testUser2.getId().toString())
                        .header("Authorization", "Bearer " + validTokenUser1))
                .andExpect(status().isOk())
                // Must still resolve strictly to User 1
                .andExpect(jsonPath("$.id", is(testUser1.getId().intValue())))
                .andExpect(jsonPath("$.email", is("secuser1@example.com")));
    }

    @Test
    void validSignedTokenForNonExistentUser_Returns401Unauthorized() throws Exception {
        // Valid token signature, but user does not exist in DB
        String nonExistentUserToken = jwtTokenProvider.generateToken("nonexistent_user_999@example.com");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + nonExistentUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }
}
