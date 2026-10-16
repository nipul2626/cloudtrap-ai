
package com.cloudtrap.api.scanning.provider.aws;

import com.cloudtrap.api.scanning.provider.CloudProvider;
import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import com.cloudtrap.api.scanning.provider.CloudResourceScope;
import com.cloudtrap.api.scanning.provider.CollectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AwsStsCloudProviderAdapterTest {

    private static final String ACCOUNT_ID = "123456789012";

    private static final UUID WORKSPACE_ID =
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private StsClient stsClient;
    private S3Client s3Client;
    private AwsStsCloudProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        stsClient = mock(StsClient.class);
        s3Client = mock(S3Client.class);

        AwsS3BucketCollector s3Collector =
                new AwsS3BucketCollector(s3Client);

        adapter = new AwsStsCloudProviderAdapter(
                stsClient,
                s3Collector);
    }

    @Test
    void identifiesAsAwsProvider() {
        assertEquals(CloudProvider.AWS, adapter.provider());
    }

    @Test
    void validatesMatchingAwsAccount() throws Exception {
        when(stsClient.getCallerIdentity()).thenReturn(
                GetCallerIdentityResponse.builder()
                        .account(ACCOUNT_ID)
                        .arn("arn:aws:iam::123456789012:user/test")
                        .userId("test-user")
                        .build()
        );

        CloudProviderAdapter.AccountValidation result =
                adapter.validateAccount(scope());

        assertTrue(result.valid());
        assertEquals(CloudProvider.AWS, result.provider());
        assertEquals(ACCOUNT_ID, result.accountId());

        verify(stsClient, times(1)).getCallerIdentity();
    }

    @Test
    void rejectsDifferentAuthenticatedAccount() throws Exception {
        when(stsClient.getCallerIdentity()).thenReturn(
                GetCallerIdentityResponse.builder()
                        .account("999999999999")
                        .arn("arn:aws:iam::999999999999:user/test")
                        .userId("test-user")
                        .build()
        );

        CloudProviderAdapter.AccountValidation result =
                adapter.validateAccount(scope());

        assertFalse(result.valid());
        assertEquals(ACCOUNT_ID, result.accountId());
    }

    @Test
    void reportsS3InventoryFailureInsteadOfCleanCoverage() {
        when(s3Client.listBucketsPaginator(any(ListBucketsRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("Access denied")
                        .statusCode(403)
                        .build());

        CloudProviderAdapter.CollectionRequest request =
                new CloudProviderAdapter.CollectionRequest(
                        WORKSPACE_ID,
                        scope(),
                        "aws-s3",
                        "1.0.0",
                        Duration.ofSeconds(20),
                        100
                );

        CollectionResult result = adapter.collect(request);

        assertEquals(CollectionResult.Coverage.FAILED, result.coverage());
        assertTrue(result.assets().isEmpty());
        assertFalse(result.errors().isEmpty());

        verify(s3Client).listBucketsPaginator(
                any(ListBucketsRequest.class));
        verifyNoInteractions(stsClient);
    }

    private static CloudResourceScope scope() {
        return new CloudResourceScope(
                ACCOUNT_ID,
                List.of("us-east-1"),
                List.of("s3")
        );
    }
}
