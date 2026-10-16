package com.cloudtrap.api.scanning.rules;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Executes an immutable, uniquely identified rule pack in stable order. */
@Component
public class DeterministicRuleEngine {
    private final List<SecurityRule> rules;
    private final String ruleSetVersion;

    public DeterministicRuleEngine(List<SecurityRule> rules) {
        List<SecurityRule> ordered = new ArrayList<>(rules == null ? List.of() : rules);
        ordered.sort(Comparator.comparing(SecurityRule::id).thenComparing(SecurityRule::version));
        Set<String> identities = new HashSet<>();
        for (SecurityRule rule : ordered) {
            if (rule == null || rule.id() == null || rule.version() == null
                    || rule.id().isBlank() || rule.version().isBlank()) {
                throw new IllegalStateException("Every registered security rule must have an ID and version");
            }
            if (!identities.add(rule.id() + "@" + rule.version())) {
                throw new IllegalStateException("Duplicate security rule registration: " + rule.id());
            }
        }
        this.rules = List.copyOf(ordered);
        this.ruleSetVersion = ordered.stream()
                .map(rule -> rule.id() + "@" + rule.version())
                .reduce((left, right) -> left + "," + right)
                .orElse("empty");
    }

    public List<RuleFinding> evaluate(NormalizedAsset asset) {
        List<RuleFinding> findings = new ArrayList<>();
        for (SecurityRule rule : rules) {
            rule.evaluate(asset).ifPresent(findings::add);
        }
        return List.copyOf(findings);
    }

    public String ruleSetVersion() {
        return ruleSetVersion;
    }

    public int ruleCount() {
        return rules.size();
    }
}
