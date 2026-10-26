package com.example.crud.config;

import com.example.crud.dto.ErrorResponseDTO;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    @Bean
    LoginRateLimiter loginRateLimiter(
            @Value("${app.security.login.max-attempts:10}") int attempts,
            @Value("${app.security.login.max-clients:10000}") int clients,
            @Value("${app.security.login.window-seconds:60}") long seconds) {
        return new LoginRateLimiter(attempts, clients, Duration.ofSeconds(seconds), System::nanoTime);
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper mapper, UserDetailsService accounts,
            LoginRateLimiter limiter) throws Exception {
        var entryPoint = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, error) -> {
            response.setHeader("WWW-Authenticate", "Basic realm=\"crud-api\"");
            writeError(mapper, response, HttpStatus.UNAUTHORIZED, "Autenticação necessária ou credenciais inválidas");
        };
        return http
                .addFilterBefore(new LoginRateLimitFilter(limiter, mapper), CsrfFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtAccountConverter(accounts)))
                        .authenticationEntryPoint(entryPoint))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((request, response, error) -> writeError(mapper, response,
                                HttpStatus.FORBIDDEN, error instanceof CsrfException
                                        ? "Token CSRF ausente ou inválido" : "Permissão insuficiente")))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/auth/login")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CookieRequestHandler()))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.HEAD, "/actuator/health/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/csrf").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users", "/users/**").hasAnyRole("READER", "ADMIN")
                        .requestMatchers(HttpMethod.HEAD, "/users", "/users/**").hasAnyRole("READER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/users/*").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .build();
    }

    static void writeError(ObjectMapper mapper, HttpServletResponse response,
            HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ErrorResponseDTO(status.value(), status.getReasonPhrase(),
                message, LocalDateTime.now()));
    }

    /** Keeps XOR protection for rendered tokens and accepts the raw cookie token in the Swagger header. */
    static final class CookieRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestAttributeHandler plain = new CsrfTokenRequestAttributeHandler();
        private final XorCsrfTokenRequestAttributeHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> token) {
            xor.handle(request, response, token);
            CsrfToken raw = token.get();
            raw.getToken(); // Materialize the deferred cookie for Swagger.
            request.setAttribute("rawCsrfToken", raw);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
            String header = request.getHeader(token.getHeaderName());
            return header != null && !header.isBlank() ? plain.resolveCsrfTokenValue(request, token)
                    : xor.resolveCsrfTokenValue(request, token);
        }
    }
}

