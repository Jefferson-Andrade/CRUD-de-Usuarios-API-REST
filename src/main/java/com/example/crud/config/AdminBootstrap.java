package com.example.crud.config;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.dao.DuplicateKeyException;

/** Creates an initial administrator; never resets an existing account or logs credentials. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminBootstrap(JdbcTemplate jdbc, PasswordEncoder encoder,
            @Value("${app.security.bootstrap.username:}") String username,
            @Value("${app.security.bootstrap.password:}") String password) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isEmpty() && password.isEmpty()) return;
        if (!username.matches("[A-Za-z0-9._-]{3,100}") || password.isBlank()
                || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configure bootstrap username (3–100 letras, números, ., _, -) "
                    + "e password (mínimo 12 caracteres e máximo 72 bytes UTF-8)");
        }
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM auth_accounts WHERE username = ?",
                Integer.class, username);
        if (existing != null && existing > 0) return;
        try {
            jdbc.update("INSERT INTO auth_accounts (username, password_hash, role, enabled) VALUES (?, ?, 'ADMIN', true)",
                    username, encoder.encode(password));
        } catch (DuplicateKeyException concurrentStartup) {
            // Another instance created the same account first. Preserve that account unchanged.
        }
    }
}
