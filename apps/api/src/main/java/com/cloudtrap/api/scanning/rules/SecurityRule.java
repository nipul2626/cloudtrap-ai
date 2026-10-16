package com.cloudtrap.api.scanning.rules;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;

import java.util.Optional;

/**
 * Pure deterministic rule contract. Implementations must not perform network I/O,
 * mutate assets, access credentials, or invoke AI services.
 */
public interface SecurityRule {
    String id();
    String version();
    Optional<RuleFinding> evaluate(NormalizedAsset asset);
}
