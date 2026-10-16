package com.cloudtrap.api.scanning.provider;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Explicit scope for a cloud inventory collection.
 *
 * This type describes what to inspect. It does not authorize access by itself;
 * authorization and cloud-account ownership must be checked separately.
 */
public record CloudResourceScope(
        String accountId,
        List<String> regions,
        List<String> resourceTypes
) {
    private static final int MAX_REGIONS = 100;
    private static final int MAX_RESOURCE_TYPES = 100;

    private static final Pattern AWS_ACCOUNT_ID =
            Pattern.compile("[0-9]{12}");

    private static final Pattern AZURE_SUBSCRIPTION_ID =
            Pattern.compile(
                    "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                            + "[0-9a-f]{4}-[0-9a-f]{12}"
            );

    private static final Pattern GCP_PROJECT_ID =
            Pattern.compile(
                    "[a-z][a-z0-9-]{4,28}[a-z0-9]"
            );

    private static final Pattern REGION =
            Pattern.compile("[a-zA-Z0-9-]{1,64}");

    private static final Pattern RESOURCE_TYPE =
            Pattern.compile("[a-zA-Z][a-zA-Z0-9._:/-]{0,119}");

    public CloudResourceScope {
        accountId = requireAccountId(accountId);

        regions = immutableValidatedList(
                regions,
                "regions",
                MAX_REGIONS,
                REGION
        );

        resourceTypes = immutableValidatedList(
                resourceTypes,
                "resourceTypes",
                MAX_RESOURCE_TYPES,
                RESOURCE_TYPE
        );
    }

    private static String requireAccountId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Cloud account identifier is required"
            );
        }

        String normalized = value.trim();

        if (normalized.length() > 128) {
            throw new IllegalArgumentException(
                    "Cloud account identifier exceeds its limit"
            );
        }

        return normalized;
    }

    private static List<String> immutableValidatedList(
            List<String> values,
            String field,
            int maximum,
            Pattern pattern
    ) {
        Objects.requireNonNull(values, field);

        if (values.size() > maximum) {
            throw new IllegalArgumentException(
                    field + " exceeds its maximum size"
            );
        }

        List<String> copy = List.copyOf(values);

        for (String value : copy) {
            if (value == null
                    || value.isBlank()
                    || !pattern.matcher(value).matches()) {
                throw new IllegalArgumentException(
                        field + " contains an invalid value"
                );
            }
        }

        if (copy.stream().distinct().count() != copy.size()) {
            throw new IllegalArgumentException(
                    field + " must not contain duplicates"
            );
        }

        return copy;
    }

    /**
     * Validates the account identifier format for the selected provider.
     * This is syntactic validation only; the provider API must verify that
     * the account exists and that the authenticated credentials can access it.
     */
    public void validateFor(CloudProvider provider) {
        Objects.requireNonNull(provider, "provider");

        boolean valid = switch (provider) {
            case AWS -> AWS_ACCOUNT_ID.matcher(accountId).matches();
            case AZURE -> AZURE_SUBSCRIPTION_ID.matcher(accountId).matches();
            case GCP -> GCP_PROJECT_ID.matcher(accountId).matches();
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    "Account identifier format is invalid for the provider"
            );
        }
    }
}