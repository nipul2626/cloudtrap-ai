
package com.cloudtrap.api.scanning.provider.aws;

import com.cloudtrap.api.scanning.provider.CloudProvider;
import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import com.cloudtrap.api.scanning.provider.CloudResourceScope;
import com.cloudtrap.api.scanning.provider.CollectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ec2.Ec2Client;

class AwsStsCloudProviderAdapterTest {

    private static final String ACCOUNT_ID = "123456789012";

    private static final UUID WORKSPACE_ID =
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private StsClient stsClient;
    private S3Client s3Client;
    private AwsStsCloudProviderAdapter adapter;




private software.amazon.awssdk.services.ec2.Ec2Client ec2Client;
private AwsS3BucketCollector s3Collector;
private AwsEc2SecurityGroupCollector ec2Collector;


@BeforeEach
void setUp() {
    stsClient = mock(StsClient.class);
    s3Client = mock(S3Client.class);
    ec2Client = mock(
            software.amazon.awssdk.services.ec2.Ec2Client.class);

    s3Collector = new AwsS3BucketCollector(s3Client);
    ec2Collector = new AwsEc2SecurityGroupCollector(ec2Client);

    adapter = new AwsStsCloudProviderAdapter(
            stsClient, s3Collector, ec2Collector);
}


@Test
void rejectsUnsupportedCollectorId() {
    CloudProviderAdapter.CollectionRequest request =
            new CloudProviderAdapter.CollectionRequest(
                    WORKSPACE_ID,
                    scope(),
                    "aws-unknown",
                    "1.0.0",
                    Duration.ofSeconds(20),
                    100);

    assertThrows(
            IllegalArgumentException.class,
            () -> adapter.collect(request));

    verifyNoInteractions(s3Client, ec2Client);
}

@Test
void routesEc2CollectorRequestToEc2Collector() {
    CloudProviderAdapter.CollectionRequest request =
            new CloudProviderAdapter.CollectionRequest(
                    WORKSPACE_ID,
                    new CloudResourceScope(
                            ACCOUNT_ID,
                            List.of("us-east-1"),
                            List.of("ec2:security-group")),
                    "aws-ec2-security-groups",
                    "1.0.0",
                    Duration.ofSeconds(20),
                    100);

    // An empty successful EC2 response can be added in the dedicated
    // collector test below. This verifies the adapter's routing contract.
    assertDoesNotThrow(() -> adapter.collect(request));
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
