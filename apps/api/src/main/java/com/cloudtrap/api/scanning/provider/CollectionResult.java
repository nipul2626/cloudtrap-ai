package com.cloudtrap.api.scanning.provider;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable outcome of one provider collection operation.
 *
 * A successful result may still be partial. Callers must inspect coverage
 * and errors before deciding whether a scan can be marked complete.
 */
public record CollectionResult(
        String collectorId,
        String collectorVersion,
        Instant startedAt,
        Instant completedAt,
        List<NormalizedAsset> assets,
        Coverage coverage,
        List<CollectionError> errors
) {
    private static final int MAX_ASSETS_PER_RESULT = 100_000;
    private static final int MAX_ERRORS_PER_RESULT = 1_000;

    public CollectionResult {
        collectorId = required(
                collectorId,
                "collectorId",
                120
        );

        collectorVersion = required(
                collectorVersion,
                "collectorVersion",
                40
        );

        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(completedAt, "completedAt");
        Objects.requireNonNull(coverage, "coverage");

        if (completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "Collection completion cannot precede its start"
            );
        }

        assets = List.copyOf(
                Objects.requireNonNull(assets, "assets")
        );

        errors = List.copyOf(
                Objects.requireNonNull(errors, "errors")
        );

        if (assets.size() > MAX_ASSETS_PER_RESULT) {
            throw new IllegalArgumentException(
                    "Collection result exceeds the asset limit"
            );
        }

        if (errors.size() > MAX_ERRORS_PER_RESULT) {
            throw new IllegalArgumentException(
                    "Collection result exceeds the error limit"
            );
        }

        Set<String> identities = new HashSet<>();

        for (NormalizedAsset asset : assets) {
            Objects.requireNonNull(asset, "asset");

            String identity = asset.provider()
                    + "\u0000"
                    + asset.externalId();

            if (!identities.add(identity)) {
                throw new IllegalArgumentException(
                        "Collection result contains duplicate asset identities"
                );
            }
        }

        if (coverage == Coverage.COMPLETE && !errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Complete coverage cannot contain collection errors"
            );
        }

        if (coverage == Coverage.FAILED && !assets.isEmpty()) {
            throw new IllegalArgumentException(
                    "Failed collection results must not contain assets"
            );
        }

        if (coverage == Coverage.FAILED && errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Failed collection results must contain an error"
            );
        }

        if (coverage == Coverage.PARTIAL && errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Partial coverage must explain its collection errors"
            );
        }
    }

    private static String required(
            String value,
            String field,
            int maximumLength
    ) {
        if (value == null
                || value.isBlank()
                || value.trim().length() > maximumLength) {
            throw new IllegalArgumentException(
                    field + " is missing or exceeds its limit"
            );
        }

        return value.trim();
    }

    public enum Coverage {
        COMPLETE,
        PARTIAL,
        FAILED
    }
}