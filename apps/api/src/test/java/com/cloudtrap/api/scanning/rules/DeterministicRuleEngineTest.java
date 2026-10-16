package com.cloudtrap.api.scanning.rules;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicRuleEngineTest {
    @Test
    void rulesAreEvaluatedInStableOrderAndVersionIsStable() {
        SecurityRule second = rule("RULE-B", "1.0.0");
        SecurityRule first = rule("RULE-A", "2.0.0");
        DeterministicRuleEngine engine = new DeterministicRuleEngine(List.of(second, first));

        assertEquals(2, engine.ruleCount());
        assertEquals("RULE-A@2.0.0,RULE-B@1.0.0", engine.ruleSetVersion());
        assertTrue(engine.evaluate(asset()).isEmpty());
    }

    @Test
    void duplicateRuleIdentityIsRejected() {
        assertThrows(IllegalStateException.class, () -> new DeterministicRuleEngine(
                List.of(rule("RULE-A", "1.0.0"), rule("RULE-A", "1.0.0"))));
    }

    private static SecurityRule rule(String id, String version) {
        return new SecurityRule() {
            @Override public String id() { return id; }
            @Override public String version() { return version; }
            @Override public Optional<com.cloudtrap.api.scanning.domain.RuleFinding> evaluate(NormalizedAsset asset) {
                return Optional.empty();
            }
        };
    }

    private static NormalizedAsset asset() {
        return new NormalizedAsset(UUID.randomUUID(), UUID.randomUUID(), "DEMO",
                "asset-1", "compute-instance", "Instance", null,
                NormalizedAsset.Exposure.UNKNOWN, 3, Map.of());
    }
}
