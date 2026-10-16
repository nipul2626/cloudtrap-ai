
package com.cloudtrap.api.scanning.provider;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class CloudDiscoveryService {

    private final CloudProviderAdapterRegistry registry;

    public CloudDiscoveryService(CloudProviderAdapterRegistry registry) {
        this.registry = Objects.requireNonNull(
                registry, "Registry must not be null");
    }

    public DiscoveryOutcome discover(
            String providerIdentifier,
            CloudProviderAdapter.CollectionRequest request)
            throws CloudProviderAdapter.ProviderAdapterException {

        Objects.requireNonNull(request, "Collection request must not be null");

        CloudProvider provider =
                CloudProvider.fromIdentifier(providerIdentifier);

        CloudResourceScope scope = request.scope();
        scope.validateFor(provider);

        CloudProviderAdapter adapter = registry.getRequired(provider);

        Instant startedAt = Instant.now();

        CloudProviderAdapter.AccountValidation validation =
                adapter.validateAccount(scope);

        if (validation == null) {
            throw new IllegalStateException(
                    "Provider adapter returned no account validation result");
        }

        if (validation.provider() != provider
                || !scope.accountId().equals(validation.accountId())) {
            throw new IllegalStateException(
                    "Provider adapter returned mismatched account validation");
        }

        if (!validation.valid()) {
            throw new AccountValidationFailedException(
                    provider, validation.safeMessage());
        }

        CollectionResult result = adapter.collect(request);

        if (result == null) {
            throw new IllegalStateException(
                    "Provider adapter returned no collection result");
        }

        if (!request.collectorId().equals(result.collectorId())) {
            throw new IllegalStateException(
                    "Provider adapter returned a mismatched collector result");
        }

        if (!request.collectorVersion().equals(result.collectorVersion())) {
            throw new IllegalStateException(
                    "Provider adapter returned a mismatched collector version");
        }

        if (result.completedAt().isBefore(startedAt)) {
            throw new IllegalStateException(
                    "Provider adapter returned an invalid completion time");
        }

        return new DiscoveryOutcome(provider, validation, result);
    }

    public record DiscoveryOutcome(
            CloudProvider provider,
            CloudProviderAdapter.AccountValidation accountValidation,
            CollectionResult collectionResult) {

        public DiscoveryOutcome {
            Objects.requireNonNull(provider, "Provider must not be null");
            Objects.requireNonNull(
                    accountValidation, "Account validation must not be null");
            Objects.requireNonNull(
                    collectionResult, "Collection result must not be null");
        }
    }

    public static final class AccountValidationFailedException
            extends IllegalStateException {

        public AccountValidationFailedException(
                CloudProvider provider, String safeMessage) {
            super("Account validation failed for "
                    + provider.identifier() + ": " + safeMessage);
        }
    }
}
