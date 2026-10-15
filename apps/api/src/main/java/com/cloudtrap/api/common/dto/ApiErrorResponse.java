
package com.cloudtrap.api.common.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Standard, safe error representation returned by the CloudTrap API.
 *
 * <p>This type contains client-facing error information only.
 * It must never contain stack traces, SQL statements, credentials,
 * submitted passwords, or internal exception details.</p>
 *
 * <p>The HTTP status is also set on the actual HTTP response.
 * The exception handler is responsible for keeping both statuses
 * consistent.</p>
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String errorCode,
        String message,
        String path,
        String requestId,
        List<FieldViolation> fieldErrors
) {

    public ApiErrorResponse {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(error, "error must not be null");
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(path, "path must not be null");

        if (status < 400 || status > 599) {
            throw new IllegalArgumentException(
                    "Error response status must be between 400 and 599"
            );
        }

        if (error.isBlank()) {
            throw new IllegalArgumentException("error must not be blank");
        }

        if (errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode must not be blank");
        }

        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }

        if (requestId != null && requestId.isBlank()) {
            requestId = null;
        }

        fieldErrors = fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }

    /**
     * A safe description of a request-field validation failure.
     * Never include the rejected field value.
     */
    public record FieldViolation(
            String field,
            String message
    ) {
        public FieldViolation {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(message, "message must not be null");

            if (field.isBlank()) {
                throw new IllegalArgumentException(
                        "field must not be blank"
                );
            }

            if (message.isBlank()) {
                throw new IllegalArgumentException(
                        "message must not be blank"
                );
            }
        }
    }
}
