
package com.cloudtrap.api.scanning.provider;

import java.util.Locale;

/**
 * Supported real cloud providers for provider-backed security discovery.
 *
 * <p>This enum deliberately excludes DEMO: demo inventory is a scan source,
 * not a cloud provider. Provider identifiers are stable, lowercase values
 * suitable for configuration and external API contracts.</p>
 *
 * <p>Adding a provider here does not enable collection automatically.
 * A provider must also have an implemented adapter, appropriate permission
 * checks, and tests before it can be used for live scans.</p>
 */
public enum CloudProvider {

    AWS("aws"),
    AZURE("azure"),
    GCP("gcp");

    private final String identifier;

    CloudProvider(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the stable, lowercase provider identifier.
     *
     * @return provider identifier such as {@code aws}
     */
    public String identifier() {
        return identifier;
    }

    /**
     * Resolves a provider from its external identifier.
     *
     * @param value provider identifier; surrounding whitespace is ignored
     * @return matching provider
     * @throws IllegalArgumentException if the value is null, blank, or unknown
     */
    public static CloudProvider fromIdentifier(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Cloud provider identifier must not be blank"
            );
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        for (CloudProvider provider : values()) {
            if (provider.identifier.equals(normalized)) {
                return provider;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported cloud provider identifier"
        );
    }
}
