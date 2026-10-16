package com.cloudtrap.api.scanning.rules;

import com.cloudtrap.api.scanning.domain.EvidenceItem;
import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import com.cloudtrap.api.scanning.domain.ScanSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Uses a normalized boolean attribute; provider adapters own provider-specific mapping. */
@Component
public class UnencryptedStorageRule implements SecurityRule {
    @Override public String id() { return "CLOUDTRAP-STORAGE-001"; }
    @Override public String version() { return "1.0.0"; }

    @Override
    public Optional<RuleFinding> evaluate(NormalizedAsset asset) {
        String type = asset.assetType().toLowerCase(Locale.ROOT);
        if (!type.contains("storage") && !type.contains("bucket") && !type.contains("disk")) {
            return Optional.empty();
        }
        Object encryption = asset.attributes().get("encryptionEnabled");
        if (!(encryption instanceof Boolean enabled) || enabled) return Optional.empty();

        return Optional.of(new RuleFinding(
                id(), version(),
                "Storage encryption is disabled",
                "The normalized inventory explicitly reports that encryption at rest is disabled.",
                "DATA_PROTECTION",
                ScanSeverity.HIGH,
                List.of(new EvidenceItem("encryptionEnabled", "false", "true",
                        "Encryption status is explicitly false in normalized inventory.")),
                "Enable supported encryption at rest",
                List.of(
                        "Review data classification and the service's encryption options.",
                        "Enable encryption using an approved key-management configuration.",
                        "Validate application compatibility and verify encryption status after the change."
                )
        ));
    }
}
