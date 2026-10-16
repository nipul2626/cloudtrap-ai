package com.cloudtrap.api.scanning.risk;

import com.cloudtrap.api.scanning.domain.ScanSeverity;

import java.util.List;

/** Reproducible risk score and its explainable factors. */
public record RiskAssessment(int score, String formulaVersion, List<Factor> factors) {
    public RiskAssessment {
        if (score < 0 || score > 100) throw new IllegalArgumentException("score must be 0..100");
        if (formulaVersion == null || formulaVersion.isBlank()) throw new IllegalArgumentException("formulaVersion is required");
        factors = List.copyOf(factors);
    }

    public record Factor(String name, int points, String reason) {
        public Factor {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("factor name is required");
            if (reason == null || reason.isBlank()) throw new IllegalArgumentException("factor reason is required");
        }
    }
}
