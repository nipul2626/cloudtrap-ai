
package com.cloudtrap.api.scanning.worker;

import com.cloudtrap.api.scanning.provider.CloudDiscoveryService;
import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import com.cloudtrap.api.scanning.provider.CloudResourceScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AwsScanExecutionService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final CloudDiscoveryService discoveryService;

    public AwsScanExecutionService(
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            CloudDiscoveryService discoveryService) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.mapper = Objects.requireNonNull(mapper);
        this.discoveryService = Objects.requireNonNull(discoveryService);
    }

    public CloudDiscoveryService.DiscoveryOutcome execute(
            UUID scanId,
            UUID workspaceId) throws Exception {

        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT s.cloud_account_id,
                       s.collector_id,
                       s.collector_version,
                       s.collection_regions,
                       s.collection_resource_types,
                       s.maximum_assets,
                       s.collection_timeout_seconds,
                       ca.account_identifier,
                       ca.provider,
                       ca.status
                FROM scans s
                JOIN cloud_accounts ca
                  ON ca.id = s.cloud_account_id
                 AND ca.workspace_id = s.workspace_id
                WHERE s.id = ?
                  AND s.workspace_id = ?
                  AND s.source = 'AWS'
                """,
                scanId,
                workspaceId
        );

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "AWS scan or associated cloud account was not found");
        }

        Map<String, Object> row = rows.getFirst();

        if (!"AWS".equals(row.get("provider"))
                || !"ACTIVE".equals(row.get("status"))) {
            throw new IllegalStateException(
                    "The AWS cloud account is not active");
        }

        String accountId = Objects.toString(
                row.get("account_identifier"), "");

        String collectorId = Objects.toString(
                row.get("collector_id"), "");

        String collectorVersion = Objects.toString(
                row.get("collector_version"), "");

        int maximumAssets = ((Number) row.get("maximum_assets")).intValue();

        int timeoutSeconds = ((Number)
                row.get("collection_timeout_seconds")).intValue();

        List<String> regions = readStringList(
                row.get("collection_regions"));

        List<String> resourceTypes = readStringList(
                row.get("collection_resource_types"));

        CloudResourceScope scope = new CloudResourceScope(
                accountId,
                regions,
                resourceTypes
        );

        CloudProviderAdapter.CollectionRequest request =
                new CloudProviderAdapter.CollectionRequest(
                        workspaceId,
                        scope,
                        collectorId,
                        collectorVersion,
                        Duration.ofSeconds(timeoutSeconds),
                        maximumAssets
                );

        return discoveryService.discover("aws", request);
    }

    private List<String> readStringList(Object value) throws Exception {
        if (value == null) {
            return List.of();
        }

        if (value instanceof String json) {
            return mapper.readValue(
                    json,
                    mapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class)
            );
        }

        // PostgreSQL drivers can expose JSONB through driver-specific
        // objects. Convert the stored representation through Jackson.
        return mapper.readValue(
                value.toString(),
                mapper.getTypeFactory()
                        .constructCollectionType(List.class, String.class)
        );
    }
}
