
package com.cloudtrap.api.scanning.worker;

import com.cloudtrap.api.scanning.risk.DeterministicRiskScorer;
import com.cloudtrap.api.scanning.rules.DeterministicRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScanWorkerTest {

    private JdbcTemplate jdbc;
    private DeterministicRuleEngine rules;
    private DeterministicRiskScorer scorer;
    private TransactionTemplate transactionTemplate;
    private AwsScanExecutionService awsExecutionService;
    private ScanWorker worker;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        rules = mock(DeterministicRuleEngine.class);
        scorer = mock(DeterministicRiskScorer.class);
        transactionTemplate = mock(TransactionTemplate.class);
        awsExecutionService = mock(AwsScanExecutionService.class);

        worker = new ScanWorker(
                jdbc,
                new ObjectMapper(),
                rules,
                scorer,
                transactionTemplate,
                awsExecutionService,
                new ClassPathResource("datasets/default-demo.json")
        );
    }

    @Test
    void shouldReturnNormallyWhenQueueIsEmpty() {
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> worker.poll());

        verify(jdbc).update(contains("SCAN_RETRIES_EXHAUSTED"));
        verifyNoInteractions(awsExecutionService);
        verifyNoInteractions(transactionTemplate);
    }

    @Test
    void shouldRouteAwsScanToAwsExecutionService() throws Exception {
        UUID scanId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Map<String, Object> job = Map.of(
                "id", scanId,
                "workspace_id", workspaceId,
                "dataset_id", "aws-s3",
                "source", "AWS"
        );

        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(job));

        when(jdbc.queryForObject(
                contains("SELECT collection_timeout_seconds"),
                eq(Integer.class),
                any(Object[].class)
        )).thenReturn(300);

        when(jdbc.update(
                contains("SET lease_expires_at"),
                any(Object[].class)
        )).thenReturn(1);

        doThrow(new IllegalStateException("Simulated AWS failure"))
                .when(awsExecutionService)
                .execute(scanId, workspaceId);

        assertDoesNotThrow(() -> worker.poll());

        verify(awsExecutionService).execute(scanId, workspaceId);

        verify(jdbc).update(
                contains("SET status = CASE"),
                any(Object[].class)
        );

        verifyNoInteractions(transactionTemplate);
    }
}
