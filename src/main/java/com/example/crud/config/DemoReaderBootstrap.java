package com.example.crud.config;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Opt-in reader provisioning; never promotes, enables or resets an existing account. */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoReaderBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String adminUsername;

    public DemoReaderBootstrap(JdbcTemplate jdbc, PasswordEncoder encoder,
            @Value("${app.demo.reader.username:}") String username,
            @Value("${app.demo.reader.password:}") String password,
            @Value("${app.security.bootstrap.username:}") String adminUsername) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
        this.adminUsername = adminUsername;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.equals(adminUsername)) {
            throw new IllegalStateException("Demo READER e ADMIN precisam de usuarios distintos");
        }
        if (!username.matches("[A-Za-z0-9._-]{3,100}") || password.isBlank()
                || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Demo exige usuario READER valido e senha de 12 caracteres a 72 bytes UTF-8");
        }
        if (existingReader()) return;
        try {
            jdbc.update("INSERT INTO auth_accounts (username, password_hash, role, enabled) VALUES (?, ?, 'READER', true)",
                    username, encoder.encode(password));
        } catch (DuplicateKeyException concurrentStartup) {
            // A concurrent bootstrap must also have created an enabled READER.
            if (!existingReader()) throw concurrentStartup;
        }
    }

    private boolean existingReader() {
        var accounts = jdbc.queryForList("SELECT role, enabled FROM auth_accounts WHERE username = ?", username);
        if (accounts.isEmpty()) return false;
        var account = accounts.getFirst();
        if (!"READER".equals(account.get("role")) || !Boolean.TRUE.equals(account.get("enabled"))) {
            throw new IllegalStateException("Conta de demonstracao existente precisa ser READER ativa");
        }
        return true;
    }
}
