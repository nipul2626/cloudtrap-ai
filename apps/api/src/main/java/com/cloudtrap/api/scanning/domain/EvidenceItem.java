package com.cloudtrap.api.scanning.domain;

import java.util.Objects;

/** Safe, structured evidence; do not include raw provider payloads or secrets. */
public record EvidenceItem(String key, String observedValue, String expectedValue, String explanation) {
    public EvidenceItem {
        key = required(key, "key", 120);
        observedValue = safe(observedValue, 500);
        expectedValue = safe(expectedValue, 500);
        explanation = required(explanation, "explanation", 1000);
    }

    private static String required(String value, String field, int max) {
        String normalized = safe(value, max);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String safe(String value, int max) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.length() > max) {
            throw new IllegalArgumentException("Evidence field exceeds its limit");
        }
        return normalized;
    }
}
