
package com.cloudtrap.api.scanning.provider;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class CloudProviderAdapterRegistry {

    private final Map<CloudProvider, CloudProviderAdapter> adapters;

    public CloudProviderAdapterRegistry(List<CloudProviderAdapter> adapters) {
        Objects.requireNonNull(adapters, "Adapters must not be null");

        Map<CloudProvider, CloudProviderAdapter> registered =
                new EnumMap<>(CloudProvider.class);

        for (CloudProviderAdapter adapter : adapters) {
            Objects.requireNonNull(adapter, "Adapter must not be null");

            CloudProvider provider = Objects.requireNonNull(
                    adapter.provider(),
                    "Adapter provider must not be null"
            );

            if (registered.putIfAbsent(provider, adapter) != null) {
                throw new IllegalStateException(
                        "Multiple adapters registered for provider: "
                                + provider.identifier()
                );
            }
        }

        this.adapters = Map.copyOf(registered);
    }

    public CloudProviderAdapter getRequired(CloudProvider provider) {
        Objects.requireNonNull(provider, "Provider must not be null");

        CloudProviderAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new UnsupportedProviderException(provider);
        }

        return adapter;
    }

    public boolean supports(CloudProvider provider) {
        Objects.requireNonNull(provider, "Provider must not be null");
        return adapters.containsKey(provider);
    }

    public static final class UnsupportedProviderException
            extends IllegalArgumentException {

        public UnsupportedProviderException(CloudProvider provider) {
            super("No adapter is registered for provider: "
                    + provider.identifier());
        }
    }
}
