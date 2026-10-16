package com.cloudtrap.api.scanning.provider;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Safe, structured description of a collection failure.
 *
 * Never store raw provider responses, credentials, request headers,
 * access tokens, or unfiltered exception messages in this record.
 */
public record CollectionError(
        String collectorId,
        ErrorCode code,
        boolean retryable,
        String safeMessage
) {
    private static final Pattern COLLECTOR_ID =
            Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9._-]{0,119}");

    public CollectionError {
        if (collectorId == null
                || !COLLECTOR_ID.matcher(collectorId).matches()) {
            throw new IllegalArgumentException(
                    "Invalid collector identifier"
            );
        }

        Objects.requireNonNull(code, "code");

        if (safeMessage == null || safeMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "A safe error message is required"
            );
        }

        safeMessage = safeMessage.trim();

        if (safeMessage.length() > 500) {
            throw new IllegalArgumentException(
                    "Safe error message exceeds its limit"
            );
        }
    }

    public enum ErrorCode {
        PERMISSION_DENIED,
        ACCOUNT_NOT_FOUND,
        INVALID_SCOPE,
        PROVIDER_THROTTLED,
        PROVIDER_UNAVAILABLE,
        REQUEST_TIMEOUT,
        INVALID_PROVIDER_RESPONSE,
        UNSUPPORTED_RESOURCE_TYPE,
        RESOURCE_LIMIT_EXCEEDED,
        COLLECTION_CANCELLED,
        INTERNAL_COLLECTION_ERROR
    }
}