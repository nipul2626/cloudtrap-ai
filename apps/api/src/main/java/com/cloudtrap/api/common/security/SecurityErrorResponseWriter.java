
package com.cloudtrap.api.common.security;

import com.cloudtrap.api.common.dto.ApiErrorResponse;
import com.cloudtrap.api.common.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication is required"
        );
    }

    public void writeForbidden(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access to this resource is forbidden"
        );
    }

    public void writeTooManyRequests(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {
        response.setHeader(
                "Retry-After",
                Long.toString(Math.max(1, retryAfterSeconds))
        );

        write(
                request,
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED",
                "Too many login attempts. Please try again later."
        );
    }

    public void writeServiceUnavailable(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        write(
                request,
                response,
                HttpStatus.SERVICE_UNAVAILABLE,
                "AUTHENTICATION_UNAVAILABLE",
                "Authentication is temporarily unavailable"
        );
    }

    private void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String errorCode,
            String message
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        Object attribute = request.getAttribute(
                RequestIdFilter.REQUEST_ATTRIBUTE
        );

        String requestId =
                attribute instanceof String value && !value.isBlank()
                        ? value
                        : UUID.randomUUID().toString();

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                errorCode,
                message,
                request.getRequestURI(),
                requestId,
                List.of()
        );

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(RequestIdFilter.HEADER_NAME, requestId);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
