
package com.cloudtrap.api.scanning.provider.aws;

import com.cloudtrap.api.scanning.provider.CloudProvider;
import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import com.cloudtrap.api.scanning.provider.CloudResourceScope;
import com.cloudtrap.api.scanning.provider.CollectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AwsStsCloudProviderAdapterTest {

    private static final String ACCOUNT_ID = "123456789012";

    private StsClient stsClient;
    private AwsStsCloudProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        stsClient = mock(StsClient.class);
        adapter = new AwsStsCloudProviderAdapter(stsClient);
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
    void doesNotReportUnimplementedCollectionAsSuccessful() {
        CloudProviderAdapter.CollectionRequest request =
                new CloudProviderAdapter.CollectionRequest(
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
