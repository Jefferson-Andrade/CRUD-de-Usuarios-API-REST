package com.example.crud;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(HealthEndpointTest.SimulatedHealth.class)
@ExtendWith(OutputCaptureExtension.class)
class HealthEndpointTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AtomicBoolean simulatedDown;

    @AfterEach
    void resetHealth() {
        simulatedDown.set(false);
    }

    @Test
    void anonymousSeesOnlyOverallStatus() throws Exception {
        String json = mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id")).andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(json).size()).isEqualTo(1);
        assertThat(mapper.readTree(json).get("status").asText()).isEqualTo("UP");
    }

    @Test
    void readerAlsoSeesOnlyOverallStatus() throws Exception {
        String json = mvc.perform(get("/actuator/health").with(user("reader").roles("READER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(json).size()).isEqualTo(1);
    }

    @Test
    void adminSeesDatabaseComponentAndDetails() throws Exception {
        mvc.perform(get("/actuator/health").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.db.details.database").value("H2"));
    }

    @Test
    void headSummaryIsPublicAndHasNoBody() throws Exception {
        mvc.perform(head("/actuator/health")).andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void anonymousCannotReadComponent() throws Exception {
        mvc.perform(get("/actuator/health/db")).andExpect(status().isUnauthorized());
    }

    @Test
    void readerCannotReadComponent() throws Exception {
        mvc.perform(get("/actuator/health/db").with(user("reader").roles("READER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadDatabaseComponent() throws Exception {
        mvc.perform(get("/actuator/health/db").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.database").value("H2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/env", "/actuator/configprops",
            "/actuator/beans", "/actuator/metrics", "/actuator/loggers"})
    void operationalEndpointsRemainDeniedEvenForAdmin(String path) throws Exception {
        mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthHasNoWriteAccessEvenWithAdminAndCsrf() throws Exception {
        mvc.perform(post("/actuator/health").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void downHealthReturns503WithoutPublicDetailsAndLogsCorrelation(CapturedOutput output) throws Exception {
        simulatedDown.set(true);
        var response = mvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable())
                .andReturn().getResponse();
        var json = mapper.readTree(response.getContentAsString());
        assertThat(json.size()).isEqualTo(1);
        assertThat(json.get("status").asText()).isEqualTo("DOWN");
        assertThat(output).contains("http_failure", "status=503", response.getHeader("X-Request-Id"));
        mvc.perform(get("/actuator/health").with(user("admin").roles("ADMIN")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.components.simulated.status").value("DOWN"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SimulatedHealth {
        @Bean AtomicBoolean simulatedDown() {
            return new AtomicBoolean();
        }

        @Bean HealthIndicator simulatedHealthIndicator(AtomicBoolean simulatedDown) {
            return () -> simulatedDown.get()
                    ? Health.down().withDetail("reason", "test-only-failure").build()
                    : Health.up().build();
        }
    }
}
