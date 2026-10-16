package com.cloudtrap.api.scanning.provider;

import java.util.UUID;
import java.time.Duration;
import java.util.Objects;

/**
 * Provider-neutral boundary for read-only cloud inventory discovery.
 *
 * Implementations must use approved credentials, validate account identity,
 * enforce resource scope, apply provider-aware pagination and throttling,
 * and honor the supplied execution timeout.
 *
 * Implementations must never mutate cloud resources.
 */
public interface CloudProviderAdapter {

    /**
     * Identifies the provider implemented by this adapter.
     */
    CloudProvider provider();

    /**
     * Validates that the configured cloud account is reachable and that
     * the current credentials resolve to the expected account.
     *
     * This operation must not return credentials or secret provider data.
     */
    AccountValidation validateAccount(CloudResourceScope scope)
            throws ProviderAdapterException;

    /**
     * Collects normalized resources within the supplied scope.
     *
     * The implementation must report incomplete coverage explicitly rather
     * than representing failed or skipped discovery as a clean result.
     */
    CollectionResult collect(
            CollectionRequest request
    ) throws ProviderAdapterException;

    /**
     * Safe result of validating a provider account.
     */
    record AccountValidation(
            CloudProvider provider,
            String accountId,
            boolean valid,
            String safeMessage
    ) {
        public AccountValidation {
            Objects.requireNonNull(provider, "provider");

            if (accountId == null || accountId.isBlank()) {
                throw new IllegalArgumentException(
                        "Validated account identifier is required"
                );
            }

            accountId = accountId.trim();

            if (accountId.length() > 128) {
                throw new IllegalArgumentException(
                        "Account identifier exceeds its limit"
                );
            }

            if (safeMessage == null || safeMessage.isBlank()) {
                throw new IllegalArgumentException(
                        "A safe validation message is required"
                );
            }

            safeMessage = safeMessage.trim();

            if (safeMessage.length() > 500) {
                throw new IllegalArgumentException(
                        "Validation message exceeds its limit"
                );
            }
        }
    }

    /**
     * Bounded collection request.
     */
    record CollectionRequest(
            UUID workspaceId,
            CloudResourceScope scope,
            String collectorId,
            String collectorVersion,
            Duration timeout,
            int maximumAssets
    ) {
        public CollectionRequest {
            Objects.requireNonNull(workspaceId, "workspaceId");
            Objects.requireNonNull(scope, "scope");

            if (collectorId == null
                    || !collectorId.matches(
                            "[a-zA-Z0-9][a-zA-Z0-9._-]{0,119}")) {
                throw new IllegalArgumentException(
                        "Invalid collector identifier");
            }

            if (collectorVersion == null
                    || collectorVersion.isBlank()
                    || collectorVersion.length() > 40) {
                throw new IllegalArgumentException(
                        "Invalid collector version");
            }

            collectorVersion = collectorVersion.trim();

            Objects.requireNonNull(timeout, "timeout");

            if (timeout.isZero()
                    || timeout.isNegative()
                    || timeout.compareTo(Duration.ofMinutes(30)) > 0) {
                throw new IllegalArgumentException(
                        "Collection timeout must be greater than zero "
                                + "and no more than 30 minutes");
            }

            if (maximumAssets < 1 || maximumAssets > 100_000) {
                throw new IllegalArgumentException(
                        "maximumAssets must be between 1 and 100000");
            }
        }
    }

    /**
     * Safe adapter-level failure. Detailed exceptions should be logged
     * internally with appropriate redaction and correlation identifiers.
     */
    final class ProviderAdapterException extends Exception {

        private final CollectionError.ErrorCode code;
        private final boolean retryable;

        public ProviderAdapterException(
                CollectionError.ErrorCode code,
                boolean retryable,
                String safeMessage
        ) {
            super(validateMessage(safeMessage));
            this.code = Objects.requireNonNull(code, "code");
            this.retryable = retryable;
        }

        public CollectionError.ErrorCode code() {
            return code;
        }

        public boolean retryable() {
            return retryable;
        }

        private static String validateMessage(String message) {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException(
                        "A safe adapter error message is required"
                );
            }

            String normalized = message.trim();

            if (normalized.length() > 500) {
                throw new IllegalArgumentException(
                        "Adapter error message exceeds its limit"
                );
            }

            return normalized;
        }
    }
}