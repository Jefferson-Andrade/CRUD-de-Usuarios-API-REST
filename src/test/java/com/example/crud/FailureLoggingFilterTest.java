package com.example.crud;

import com.example.crud.config.FailureLoggingFilter;
import jakarta.servlet.ServletException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(OutputCaptureExtension.class)
class FailureLoggingFilterTest {
    private final FailureLoggingFilter filter = new FailureLoggingFilter();

    @Test
    void serverErrorLogsCorrelationWithoutUrlHeadersBodyOrQuery(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("GET", "/users/fixture-secret-path");
        request.setQueryString("password=fixture-secret-query");
        request.addHeader("Authorization", "Basic fixture-secret-header");
        request.setContent("fixture-secret-body".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> response.setStatus(500));
        String requestId = response.getHeader("X-Request-Id");
        UUID.fromString(requestId);
        assertThat(output).contains("http_failure", "status=500", requestId)
                .doesNotContain("fixture-secret");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void unhandledFailureLogsOnlyTypeAndIsRethrown(CapturedOutput output) {
        var request = new MockHttpServletRequest("GET", "/users");
        var response = new MockHttpServletResponse();
        assertThrows(ServletException.class, () -> filter.doFilter(request, response,
                (req, res) -> { throw new ServletException("fixture-secret-exception"); }));
        assertThat(output).contains("http_failure", "failureType=ServletException",
                response.getHeader("X-Request-Id")).doesNotContain("fixture-secret-exception");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void successfulRequestDoesNotLogFailureAndRestoresCallerContext(CapturedOutput output) throws Exception {
        MDC.put("requestId", "previous-context");
        try {
            var request = new MockHttpServletRequest("GET", "/users");
            request.addHeader("X-Request-Id", "untrusted-caller-id");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> response.setStatus(200));
            assertThat(response.getHeader("X-Request-Id")).isNotEqualTo("untrusted-caller-id");
            assertThat(MDC.get("requestId")).isEqualTo("previous-context");
            assertThat(output).doesNotContain("http_failure");
        } finally {
            MDC.remove("requestId");
        }
    }
}
