package com.example.crud.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Correlates server failures without logging URL, body, headers or exception messages. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FailureLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(FailureLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        String previous = MDC.get("requestId");
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        boolean unhandled = false;
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException failure) {
            unhandled = true;
            log.warn("http_failure requestId={} method={} status=500 failureType={}",
                    requestId, request.getMethod(), failure.getClass().getSimpleName());
            throw failure;
        } finally {
            if (!unhandled && response.getStatus() >= 500) {
                log.warn("http_failure requestId={} method={} status={}",
                        requestId, request.getMethod(), response.getStatus());
            }
            if (previous == null) MDC.remove("requestId");
            else MDC.put("requestId", previous);
        }
    }
}
