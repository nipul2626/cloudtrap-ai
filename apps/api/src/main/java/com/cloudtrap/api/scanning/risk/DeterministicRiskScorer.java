package com.cloudtrap.api.scanning.risk;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import com.cloudtrap.api.scanning.domain.ScanSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Versioned heuristic score, not CVSS. The formula is pure and stable:
 * severity base + public-exposure modifier + criticality modifier, clamped to 0..100.
 */
@Component
public class DeterministicRiskScorer {
    public static final String FORMULA_VERSION = "cloudtrap-risk-v1";

    public RiskAssessment score(RuleFinding finding, NormalizedAsset asset) {
        int base = switch (finding.severity()) {
            case CRITICAL -> 85;
            case HIGH -> 70;
            case MEDIUM -> 45;
            case LOW -> 20;
            case INFO -> 5;
        };
        List<RiskAssessment.Factor> factors = new ArrayList<>();
        factors.add(new RiskAssessment.Factor("SEVERITY_BASE", base,
                "Base score derived from rule severity " + finding.severity()));

        int exposure = asset.exposure() == NormalizedAsset.Exposure.PUBLIC ? 10 : 0;
        factors.add(new RiskAssessment.Factor("PUBLIC_EXPOSURE", exposure,
                exposure == 10 ? "Asset is explicitly classified as publicly exposed"
                        : "No public-exposure modifier applied"));

        int criticality = (asset.criticality() - 1) * 2;
        factors.add(new RiskAssessment.Factor("ASSET_CRITICALITY", criticality,
                "Asset criticality is " + asset.criticality() + " on a 1..5 scale"));

        int score = Math.max(0, Math.min(100, base + exposure + criticality));
        return new RiskAssessment(score, FORMULA_VERSION, factors);
    }
}
