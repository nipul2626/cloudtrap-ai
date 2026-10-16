
package com.cloudtrap.api.scanning.provider.aws;

import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import com.cloudtrap.api.scanning.provider.CloudResourceScope;
import com.cloudtrap.api.scanning.provider.CollectionError;
import com.cloudtrap.api.scanning.provider.CollectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AwsEc2SecurityGroupCollectorTest {

    private static final UUID WORKSPACE_ID =
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private static final String ACCOUNT_ID = "123456789012";

    private Ec2Client ec2;
    private AwsEc2SecurityGroupCollector collector;

    @BeforeEach
    void setUp() {
        ec2 = mock(Ec2Client.class);
        collector = new AwsEc2SecurityGroupCollector(ec2);
    }

    @Test
    void rejectsUnsupportedCollectorId() {
        var request = request(
                "aws-ec2", List.of("ec2:security-group"));

        assertThrows(
                IllegalArgumentException.class,
                () -> collector.collect(request));

        verifyNoInteractions(ec2);
    }

    @Test
    void rejectsUnsupportedResourceTypes() {
        var request = request("aws-ec2-security-groups", List.of("s3"));

        assertThrows(
                IllegalArgumentException.class,
                () -> collector.collect(request));

        verifyNoInteractions(ec2);
    }

    @Test
    void reportsPermissionDeniedWithoutExposingProviderDetails() {
        when(ec2.describeSecurityGroupsPaginator(
                any(DescribeSecurityGroupsRequest.class)))
                .thenThrow(Ec2Exception.builder()
                        .message("Sensitive provider error")
                        .statusCode(403)
                        .build());

        CollectionResult result = collector.collect(request(
                "aws-ec2-security-groups",
                List.of("ec2:security-group")));

        assertEquals(CollectionResult.Coverage.FAILED, result.coverage());
        assertTrue(result.assets().isEmpty());
        assertFalse(result.errors().isEmpty());
        assertEquals(
                CollectionError.ErrorCode.PERMISSION_DENIED,
                result.errors().getFirst().code());

        assertFalse(result.errors().getFirst().safeMessage()
                .contains("Sensitive provider error"));
    }

    @Test
    void rejectsRequestForDifferentConfiguredRegion() {
        var request = new CloudProviderAdapter.CollectionRequest(
                WORKSPACE_ID,
                new CloudResourceScope(
                        ACCOUNT_ID,
                        List.of("eu-west-1"),
                        List.of("ec2:security-group")),
                "aws-ec2-security-groups",
                "1.0.0",
                Duration.ofSeconds(20),
                100);

        CollectionResult result = collector.collect(request);

        assertEquals(CollectionResult.Coverage.FAILED, result.coverage());
        assertEquals(
                CollectionError.ErrorCode.INVALID_SCOPE,
                result.errors().getFirst().code());

        verifyNoInteractions(ec2);
    }

    private static CloudProviderAdapter.CollectionRequest request(
            String collectorId,
            List<String> resourceTypes) {

        return new CloudProviderAdapter.CollectionRequest(
                WORKSPACE_ID,
                new CloudResourceScope(
                        ACCOUNT_ID,
                        List.of("us-east-1"),
                        resourceTypes),
                collectorId,
                "1.0.0",
                Duration.ofSeconds(20),
                100);
    }
}
