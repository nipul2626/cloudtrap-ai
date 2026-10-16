
package com.cloudtrap.api.scanning.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CloudProviderAdapterRegistryTest {

    @Test
    void registersAndResolvesSupportedProvider() {
        CloudProviderAdapter adapter = new StubAdapter(CloudProvider.AWS);

        CloudProviderAdapterRegistry registry =
                new CloudProviderAdapterRegistry(List.of(adapter));

        assertTrue(registry.supports(CloudProvider.AWS));
        assertFalse(registry.supports(CloudProvider.GCP));
        assertSame(adapter, registry.getRequired(CloudProvider.AWS));
    }

    @Test
    void rejectsDuplicateProviderAdapters() {
        assertThrows(
                IllegalStateException.class,
                () -> new CloudProviderAdapterRegistry(List.of(
                        new StubAdapter(CloudProvider.AWS),
                        new StubAdapter(CloudProvider.AWS)
                ))
        );
    }

    @Test
    void rejectsMissingProviderAdapter() {
        CloudProviderAdapterRegistry registry =
                new CloudProviderAdapterRegistry(List.of());

        assertThrows(
                CloudProviderAdapterRegistry.UnsupportedProviderException.class,
                () -> registry.getRequired(CloudProvider.AWS)
        );
    }

    private static final class StubAdapter implements CloudProviderAdapter {

        private final CloudProvider provider;

        private StubAdapter(CloudProvider provider) {
            this.provider = provider;
        }

        @Override
        public CloudProvider provider() {
            return provider;
        }

        @Override
        public AccountValidation validateAccount(CloudResourceScope scope) {
            return new AccountValidation(
                    provider, scope.accountId(), true, "Account validated");
        }

        @Override
        public CollectionResult collect(CollectionRequest request) {
            throw new UnsupportedOperationException("Not used by this test");
        }
    }
}
