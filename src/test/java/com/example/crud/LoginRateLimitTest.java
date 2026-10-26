package com.example.crud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.security.login.max-attempts=2")
@AutoConfigureMockMvc
class LoginRateLimitTest {
    @Autowired MockMvc mvc;

    @Test
    void malformedBodiesAreLimitedAndForwardedHeadersCannotResetTheWindow() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{")
                    .with(request -> { request.setRemoteAddr("192.0.2.1"); return request; }))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "198.51.100.99").content("{}")
                .with(request -> { request.setRemoteAddr("192.0.2.1"); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.timestamp").isString());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{")
                .with(request -> { request.setRemoteAddr("192.0.2.2"); return request; }))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/actuator/health")
                .with(request -> { request.setRemoteAddr("192.0.2.1"); return request; }))
                .andExpect(status().isOk());
    }

    @Test
    void badCredentialsAreCountedBeforePasswordVerification() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"missing\",\"password\":\"incorrect\"}")
                    .with(request -> { request.setRemoteAddr("192.0.2.3"); return request; }))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"another\",\"password\":\"incorrect\"}")
                .with(request -> { request.setRemoteAddr("192.0.2.3"); return request; }))
                .andExpect(status().isTooManyRequests());
    }
}
