package com.cloudtrap.api.scanning.domain;

import java.util.List;
import java.util.Objects;

/** Immutable output of one deterministic rule evaluation. */
public record RuleFinding(
        String ruleId,
        String ruleVersion,
        String title,
        String description,
        String category,
        ScanSeverity severity,
        List<EvidenceItem> evidence,
        String remediationSummary,
        List<String> remediationSteps
) {
    public RuleFinding {
        ruleId = required(ruleId, "ruleId", 120);
        ruleVersion = required(ruleVersion, "ruleVersion", 40);
        title = required(title, "title", 240);
        description = required(description, "description", 4000);
        category = required(category, "category", 100);
        severity = Objects.requireNonNull(severity, "severity");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        if (evidence.isEmpty() || evidence.size() > 25) {
            throw new IllegalArgumentException("A finding must contain 1 to 25 evidence items");
        }
        remediationSummary = required(remediationSummary, "remediationSummary", 500);
        remediationSteps = List.copyOf(Objects.requireNonNull(remediationSteps, "remediationSteps"));
        if (remediationSteps.isEmpty() || remediationSteps.size() > 20) {
            throw new IllegalArgumentException("Remediation must contain 1 to 20 steps");
        }
        remediationSteps.forEach(step -> required(step, "remediationStep", 1000));
    }

    private static String required(String value, String field, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new IllegalArgumentException(field + " is missing or exceeds its limit");
        }
        return value.trim();
    }
}
