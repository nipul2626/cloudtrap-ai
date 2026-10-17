
package com.cloudtrap.api.scanning.worker;

import com.cloudtrap.api.scanning.provider.CloudDiscoveryService;
import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AwsScanExecutionServiceTest {

    private JdbcTemplate jdbc;
    private CloudDiscoveryService discoveryService;
    private AwsScanExecutionService service;

    private final UUID scanId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final UUID cloudAccountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        discoveryService = mock(CloudDiscoveryService.class);

        service = new AwsScanExecutionService(
                jdbc,
                new ObjectMapper(),
                discoveryService
        );
    }

    @Test
    void shouldRejectScanWhenNoAssociatedAccountExists() {
        when(jdbc.queryForList(
                anyString(),
                eq(scanId),
                eq(workspaceId)
        )).thenReturn(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(scanId, workspaceId)
        );

        verifyNoInteractions(discoveryService);
    }

    @Test
    void shouldRejectInactiveAwsAccount() {
        Map<String, Object> row = validRow();
        row.put("status", "DISABLED");

        when(jdbc.queryForList(
                anyString(),
                eq(scanId),
                eq(workspaceId)
        )).thenReturn(List.of(row));

        assertThrows(
                IllegalStateException.class,
                () -> service.execute(scanId, workspaceId)
        );

        verifyNoInteractions(discoveryService);
    }

    @Test
    void shouldRejectAccountWithWrongProvider() {
        Map<String, Object> row = validRow();
        row.put("provider", "GCP");

        when(jdbc.queryForList(
                anyString(),
                eq(scanId),
                eq(workspaceId)
        )).thenReturn(List.of(row));

        assertThrows(
                IllegalStateException.class,
                () -> service.execute(scanId, workspaceId)
        );

        verifyNoInteractions(discoveryService);
    }

    @Test
    void shouldBuildRequestFromStoredScanConfiguration() throws Exception {
        Map<String, Object> row = validRow();

        when(jdbc.queryForList(
                anyString(),
                eq(scanId),
                eq(workspaceId)
        )).thenReturn(List.of(row));

        CloudDiscoveryService.DiscoveryOutcome expected =
                mock(CloudDiscoveryService.DiscoveryOutcome.class);

        when(discoveryService.discover(
                eq("aws"),
                any(CloudProviderAdapter.CollectionRequest.class)
        )).thenReturn(expected);

        CloudDiscoveryService.DiscoveryOutcome actual =
                service.execute(scanId, workspaceId);

        assertSame(expected, actual);

        var captor =
                org.mockito.ArgumentCaptor.forClass(
                        CloudProviderAdapter.CollectionRequest.class
                );

        verify(discoveryService).discover(eq("aws"), captor.capture());

        CloudProviderAdapter.CollectionRequest request = captor.getValue();

        assertEquals(workspaceId, request.workspaceId());
        assertEquals("aws-s3", request.collectorId());
        assertEquals("1.0.0", request.collectorVersion());
        assertEquals(300, request.timeout().toSeconds());
        assertEquals(500, request.maximumAssets());

        assertEquals(
                "123456789012",
                request.scope().accountId()
        );

        assertEquals(
                List.of("us-east-1", "us-west-2"),
                request.scope().regions()
        );

        assertEquals(
                List.of("s3", "s3:bucket"),
                request.scope().resourceTypes()
        );
    }

    @Test
    void shouldAcceptJsonbListsReturnedAsStrings() throws Exception {
        Map<String, Object> row = validRow();
        row.put("collection_regions", "[\"eu-west-1\"]");
        row.put("collection_resource_types", "[\"s3\"]");

        when(jdbc.queryForList(
                anyString(),
                eq(scanId),
                eq(workspaceId)
        )).thenReturn(List.of(row));

        when(discoveryService.discover(
                eq("aws"),
                any(CloudProviderAdapter.CollectionRequest.class)
        )).thenReturn(
                mock(CloudDiscoveryService.DiscoveryOutcome.class)
        );

        service.execute(scanId, workspaceId);

        var captor =
                org.mockito.ArgumentCaptor.forClass(
                        CloudProviderAdapter.CollectionRequest.class
                );

        verify(discoveryService).discover(eq("aws"), captor.capture());

        assertEquals(
                List.of("eu-west-1"),
                captor.getValue().scope().regions()
        );

        assertEquals(
                List.of("s3"),
                captor.getValue().scope().resourceTypes()
        );
    }

    @Test
    void shouldTreatNullJsonbListsAsEmptyLists() throws Exception {
        Map<String, Object> row = validRow();
        row.put("collection_regions", null);
        row.put("collection_resource_types", null);

        when(jdbc.queryForList(
                anyString(),
                eq(scanId),
                eq(workspaceId)
        )).thenReturn(List.of(row));

        when(discoveryService.discover(
                eq("aws"),
                any(CloudProviderAdapter.CollectionRequest.class)
        )).thenReturn(
                mock(CloudDiscoveryService.DiscoveryOutcome.class)
        );

        service.execute(scanId, workspaceId);

        var captor =
                org.mockito.ArgumentCaptor.forClass(
                        CloudProviderAdapter.CollectionRequest.class
                );

        verify(discoveryService).discover(eq("aws"), captor.capture());

        assertEquals(List.of(), captor.getValue().scope().regions());
        assertEquals(List.of(), captor.getValue().scope().resourceTypes());
    }


private Map<String, Object> validRow() {
    Map<String, Object> row = new HashMap<>();

    row.put("cloud_account_id", cloudAccountId);
    row.put("collector_id", "aws-s3");
    row.put("collector_version", "1.0.0");

    // Simulate JSONB values returned by the database.
    row.put(
            "collection_regions",
            "[\"us-east-1\", \"us-west-2\"]"
    );
    row.put(
            "collection_resource_types",
            "[\"s3\", \"s3:bucket\"]"
    );

    row.put("maximum_assets", 500);
    row.put("collection_timeout_seconds", 300);
    row.put("account_identifier", "123456789012");
    row.put("provider", "AWS");
    row.put("status", "ACTIVE");

    return row;
}

}
