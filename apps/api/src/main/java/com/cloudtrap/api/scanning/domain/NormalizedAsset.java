package com.cloudtrap.api.scanning.domain;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Provider-neutral, secret-free view of an asset presented to deterministic rules.
 * Adapters must normalize provider payloads before constructing this type.
 */
public record NormalizedAsset(
        UUID id,
        UUID workspaceId,
        String provider,
        String externalId,
        String assetType,
        String name,
        String region,
        Exposure exposure,
        int criticality,
        Map<String, Object> attributes
) {
    public NormalizedAsset {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(workspaceId, "workspaceId");
        provider = required(provider, "provider", 30);
        externalId = required(externalId, "externalId", 512);
        assetType = required(assetType, "assetType", 100);
        name = required(name, "name", 255);
        if (criticality < 1 || criticality > 5) {
            throw new IllegalArgumentException("criticality must be between 1 and 5");
        }
        exposure = Objects.requireNonNullElse(exposure, Exposure.UNKNOWN);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    private static String required(String value, String field, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new IllegalArgumentException(field + " is missing or exceeds its limit");
        }
        return value.trim();
    }

    public enum Exposure { PUBLIC, PRIVATE, UNKNOWN }
}
