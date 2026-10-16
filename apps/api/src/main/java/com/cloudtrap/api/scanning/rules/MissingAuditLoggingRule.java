package com.cloudtrap.api.scanning.rules;

import com.cloudtrap.api.scanning.domain.EvidenceItem;
import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import com.cloudtrap.api.scanning.domain.ScanSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Detects missing audit logging only for asset types with an explicit normalized logging field. */
@Component
public class MissingAuditLoggingRule implements SecurityRule {
    @Override public String id() { return "CLOUDTRAP-AUDIT-001"; }
    @Override public String version() { return "1.0.0"; }

    @Override
    public Optional<RuleFinding> evaluate(NormalizedAsset asset) {
        String type = asset.assetType().toLowerCase(Locale.ROOT);
        if (!type.contains("audit") && !type.contains("trail") && !type.contains("logging")) {
            return Optional.empty();
        }
        Object enabled = asset.attributes().get("auditLoggingEnabled");
        if (!(enabled instanceof Boolean value) || value) return Optional.empty();

        return Optional.of(new RuleFinding(
                id(), version(),
                "Audit logging is disabled",
                "The normalized inventory explicitly reports that audit logging is disabled for this logging or audit resource.",
                "DETECTION_AND_RESPONSE",
                ScanSeverity.HIGH,
                List.of(new EvidenceItem("auditLoggingEnabled", "false", "true",
                        "Audit logging status is explicitly false in normalized inventory.")),
                "Enable and validate audit logging",
                List.of(
                        "Confirm the required audit events and retention policy.",
                        "Enable audit logging and configure protected log storage with appropriate access controls.",
                        "Generate a test event and verify it reaches the expected destination."
                )
        ));
    }
}
