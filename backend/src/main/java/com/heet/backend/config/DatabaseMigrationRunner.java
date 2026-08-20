package com.heet.backend.config;

import com.heet.backend.entity.User;
import com.heet.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User defaultUser = userRepository.findByEmail("default@finance.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setName("Default User");
                    u.setEmail("default@finance.com");
                    u.setPasswordHash(passwordEncoder.encode("default123"));
                    return userRepository.save(u);
                });

        Long defaultUserId = defaultUser.getId();

        try {
            jdbcTemplate.update("UPDATE categories SET user_id = ? WHERE user_id IS NULL", defaultUserId);
        } catch (Exception ignored) {
        }

        try {
            jdbcTemplate.update("UPDATE transactions SET user_id = ? WHERE user_id IS NULL", defaultUserId);
        } catch (Exception ignored) {
        }
    }
}
