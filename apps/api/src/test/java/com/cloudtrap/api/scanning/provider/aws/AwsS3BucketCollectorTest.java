
package com.cloudtrap.api.scanning.provider.aws;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import com.cloudtrap.api.scanning.provider.CloudResourceScope;
import com.cloudtrap.api.scanning.provider.CollectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AwsS3BucketCollectorTest {

    private static final UUID WORKSPACE_ID =
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private static final String ACCOUNT_ID = "123456789012";

    private S3Client s3;
    private AwsS3BucketCollector collector;

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        collector = new AwsS3BucketCollector(s3);
    }

    @Test
    void rejectsUnsupportedCollector() {
        CloudProviderAdapter.CollectionRequest request = request(
                "aws-ec2", "1.0.0", 10);

        assertThrows(
                IllegalArgumentException.class,
                () -> collector.collect(request));

        verifyNoInteractions(s3);
    }

    @Test
    void rejectsUnsupportedResourceTypes() {
        CloudProviderAdapter.CollectionRequest request =
                new CloudProviderAdapter.CollectionRequest(
                        WORKSPACE_ID,
                        new CloudResourceScope(
                                ACCOUNT_ID,
                                List.of("us-east-1"),
                                List.of("ec2")),
                        "aws-s3",
                        "1.0.0",
                        Duration.ofSeconds(20),
                        10);

        assertThrows(
                IllegalArgumentException.class,
                () -> collector.collect(request));

        verifyNoInteractions(s3);
    }

    @Test
    void reportsInventoryFailureInsteadOfCleanCoverage() {
        when(s3.listBucketsPaginator(any(ListBucketsRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("Access denied")
                        .statusCode(403)
                        .build());

        CollectionResult result = collector.collect(request(
                "aws-s3", "1.0.0", 10));

        assertEquals(CollectionResult.Coverage.FAILED, result.coverage());
        assertTrue(result.assets().isEmpty());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void rejectsWorkspaceMissingFromRequest() {
        assertThrows(
                NullPointerException.class,
                () -> new CloudProviderAdapter.CollectionRequest(
                        null,
                        new CloudResourceScope(
                                ACCOUNT_ID,
                                List.of("us-east-1"),
                                List.of("s3")),
                        "aws-s3",
                        "1.0.0",
                        Duration.ofSeconds(20),
                        10));
    }

    private static CloudProviderAdapter.CollectionRequest request(
            String collectorId, String version, int maximumAssets) {

        return new CloudProviderAdapter.CollectionRequest(
                WORKSPACE_ID,
                new CloudResourceScope(
                        ACCOUNT_ID,
                        List.of("us-east-1"),
                        List.of("s3")),
                collectorId,
                version,
                Duration.ofSeconds(20),
                maximumAssets);
    }
}
