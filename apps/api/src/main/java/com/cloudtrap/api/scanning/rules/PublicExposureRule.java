package com.cloudtrap.api.scanning.rules;

import com.cloudtrap.api.scanning.domain.EvidenceItem;
import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.domain.RuleFinding;
import com.cloudtrap.api.scanning.domain.ScanSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Flags normalized assets explicitly marked PUBLIC; unknown exposure is not treated as public. */
@Component
public class PublicExposureRule implements SecurityRule {
    @Override public String id() { return "CLOUDTRAP-ASSET-001"; }
    @Override public String version() { return "1.0.0"; }

    @Override
    public Optional<RuleFinding> evaluate(NormalizedAsset asset) {
        if (asset.exposure() != NormalizedAsset.Exposure.PUBLIC) return Optional.empty();
        return Optional.of(new RuleFinding(
                id(), version(),
                "Asset is publicly exposed",
                "The inventory adapter marked this asset as publicly exposed. Verify that public access is intentional and restricted to required services.",
                "EXPOSURE",
                ScanSeverity.HIGH,
                List.of(new EvidenceItem("exposure", "PUBLIC", "PRIVATE or explicitly justified",
                        "Normalized inventory reports public exposure.")),
                "Restrict public access to the minimum required surface",
                List.of(
                        "Confirm whether public reachability is a business requirement.",
                        "Remove broad public access and allow only approved principals or network ranges.",
                        "Re-run the scan and verify the exposure classification."
                )
        ));
    }
}
