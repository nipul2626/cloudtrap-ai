package com.cloudtrap.api.scanning.rules;

import com.cloudtrap.api.scanning.domain.EvidenceItem;
import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import com.cloudtrap.api.scanning.domain.ScanSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Flags normalized network assets only when the adapter explicitly reports an
 * unrestricted administrative port. Provider-specific protocol/port parsing
 * must happen in the adapter, not in this rule.
 */
@Component
public class OverlyPermissiveNetworkRule implements SecurityRule {
    @Override public String id() { return "CLOUDTRAP-NETWORK-001"; }
    @Override public String version() { return "1.0.0"; }

    @Override
    public Optional<RuleFinding> evaluate(NormalizedAsset asset) {
        if (!"true".equalsIgnoreCase(String.valueOf(asset.attributes().get("unrestrictedAdminAccess")))) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                id(), version(),
                "Administrative network access is unrestricted",
                "The inventory adapter identified an administrative port reachable from an unrestricted source range.",
                "NETWORK_SECURITY",
                ScanSeverity.CRITICAL,
                List.of(new EvidenceItem("unrestrictedAdminAccess", "true", "false",
                        "Provider-neutral inventory explicitly identifies unrestricted administrative access.")),
                "Restrict administrative ingress to approved management sources",
                List.of(
                        "Identify the exact rule and administrative service responsible for the exposure.",
                        "Replace unrestricted source ranges with approved, narrowly scoped management ranges.",
                        "Verify access through the approved management path and re-run the scan."
                )
        ));
    }
}
