package com.cloudtrap.api.scanning.risk;

import com.cloudtrap.api.scanning.domain.EvidenceItem;
import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import com.cloudtrap.api.scanning.domain.ScanSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicRiskScorerTest {
    private final DeterministicRiskScorer scorer = new DeterministicRiskScorer();

    @Test
    void scoreIsRepeatableAndIncludesExplainableFactors() {
        NormalizedAsset asset = asset(NormalizedAsset.Exposure.PUBLIC, 5);
        RuleFinding finding = finding(ScanSeverity.HIGH);

        RiskAssessment first = scorer.score(finding, asset);
        RiskAssessment second = scorer.score(finding, asset);

        assertEquals(first, second);
        assertEquals(88, first.score());
        assertEquals(DeterministicRiskScorer.FORMULA_VERSION, first.formulaVersion());
        assertEquals(3, first.factors().size());
    }

    @Test
    void higherSeverityDoesNotProduceLowerBaseRiskForSameAsset() {
        NormalizedAsset asset = asset(NormalizedAsset.Exposure.PRIVATE, 1);
        assertTrue(scorer.score(finding(ScanSeverity.CRITICAL), asset).score()
                > scorer.score(finding(ScanSeverity.HIGH), asset).score());
        assertTrue(scorer.score(finding(ScanSeverity.HIGH), asset).score()
                > scorer.score(finding(ScanSeverity.MEDIUM), asset).score());
        assertTrue(scorer.score(finding(ScanSeverity.MEDIUM), asset).score()
                > scorer.score(finding(ScanSeverity.LOW), asset).score());
    }

    @Test
    void publicExposureAndCriticalityOnlyIncreaseScore() {
        RuleFinding finding = finding(ScanSeverity.MEDIUM);
        int baseline = scorer.score(finding, asset(NormalizedAsset.Exposure.PRIVATE, 1)).score();
        assertTrue(scorer.score(finding, asset(NormalizedAsset.Exposure.PUBLIC, 1)).score() > baseline);
        assertTrue(scorer.score(finding, asset(NormalizedAsset.Exposure.PRIVATE, 5)).score() > baseline);
    }

    private static NormalizedAsset asset(NormalizedAsset.Exposure exposure, int criticality) {
        return new NormalizedAsset(UUID.randomUUID(), UUID.randomUUID(), "DEMO",
                "demo-asset-1", "storage-bucket", "Example asset", "test-region",
                exposure, criticality, Map.of());
    }

    private static RuleFinding finding(ScanSeverity severity) {
        return new RuleFinding("TEST-RULE", "1.0.0", "Test finding", "Test description",
                "TEST", severity,
                List.of(new EvidenceItem("test", "observed", "expected", "Explanation")),
                "Fix the test issue", List.of("Review the setting", "Apply a safe fix"));
    }
}
