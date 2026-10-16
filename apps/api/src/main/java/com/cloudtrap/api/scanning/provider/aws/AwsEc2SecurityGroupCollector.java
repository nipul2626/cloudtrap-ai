package com.cloudtrap.api.scanning.provider.aws;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.provider.CloudProvider;
import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import com.cloudtrap.api.scanning.provider.CloudResourceScope;
import com.cloudtrap.api.scanning.provider.CollectionError;
import com.cloudtrap.api.scanning.provider.CollectionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.Ipv6Range;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class AwsEc2SecurityGroupCollector {

    private static final String COLLECTOR_ID =
            "aws-ec2-security-groups";
    private static final String COLLECTOR_VERSION = "1.0.0";
    private static final int MAX_RECORDED_ERRORS = 1000;

    private final Ec2Client ec2;

    @Value("${cloudtrap.aws.ec2.region:us-east-1}")
    private String region = "us-east-1";

    public AwsEc2SecurityGroupCollector(Ec2Client ec2) {
        this.ec2 = Objects.requireNonNull(
                ec2, "EC2 client must not be null");
    }

    public CollectionResult collect(
            CloudProviderAdapter.CollectionRequest request) {

        Objects.requireNonNull(request, "Collection request is required");
        validateRequest(request);

        Instant startedAt = Instant.now();
        long deadline = System.nanoTime() + request.timeout().toNanos();

        List<NormalizedAsset> assets = new ArrayList<>();
        List<CollectionError> errors = new ArrayList<>();

        boolean inventoryFailed = false;
        boolean stoppedEarly = false;

        String configuredRegion = region == null ? "" : region.trim();

        if (configuredRegion.isBlank()) {
            return failedResult(
                    startedAt,
                    CollectionError.ErrorCode.INVALID_SCOPE,
                    false,
                    "The EC2 region is not configured");
        }

        if (!request.scope().regions().isEmpty()
                && !request.scope().regions().contains(configuredRegion)) {
            return failedResult(
                    startedAt,
                    CollectionError.ErrorCode.INVALID_SCOPE,
                    false,
                    "The configured EC2 region is outside the requested scope");
        }

        try {
            checkDeadline(deadline);

            var paginator = ec2.describeSecurityGroupsPaginator(
                    DescribeSecurityGroupsRequest.builder()
                            .maxResults(1000)
                            .build());

            for (var page : paginator) {
                checkDeadline(deadline);

                if (page.securityGroups() == null) {
                    addError(errors, error(
                            CollectionError.ErrorCode.INVALID_PROVIDER_RESPONSE,
                            false,
                            "AWS returned an invalid security-group inventory"));
                    inventoryFailed = true;
                    break;
                }

                for (SecurityGroup group : page.securityGroups()) {
                    checkDeadline(deadline);

                    if (assets.size() >= request.maximumAssets()) {
                        addError(errors, error(
                                CollectionError.ErrorCode.RESOURCE_LIMIT_EXCEEDED,
                                false,
                                "EC2 discovery reached the configured asset limit"));
                        stoppedEarly = true;
                        break;
                    }

                    if (group == null
                            || group.groupId() == null
                            || group.groupId().isBlank()) {
                        addError(errors, error(
                                CollectionError.ErrorCode.INVALID_PROVIDER_RESPONSE,
                                false,
                                "AWS returned a security group without an ID"));
                        continue;
                    }

                    assets.add(toAsset(
                            request, group, configuredRegion));
                }

                if (stoppedEarly
                        || errors.size() >= MAX_RECORDED_ERRORS) {
                    break;
                }
            }

        } catch (DeadlineReachedException exception) {
            addError(errors, error(
                    CollectionError.ErrorCode.REQUEST_TIMEOUT,
                    true,
                    "EC2 discovery exceeded its execution deadline"));
            stoppedEarly = true;

        } catch (Ec2Exception exception) {
            inventoryFailed = true;
            int status = exception.statusCode();

            if (status == 401 || status == 403) {
                addError(errors, error(
                        CollectionError.ErrorCode.PERMISSION_DENIED,
                        false,
                        "AWS denied the security-group inventory request"));

            } else if (status == 429 || status >= 500) {
                addError(errors, error(
                        CollectionError.ErrorCode.PROVIDER_UNAVAILABLE,
                        true,
                        "AWS could not complete the security-group inventory request"));

            } else {
                addError(errors, error(
                        CollectionError.ErrorCode.INTERNAL_COLLECTION_ERROR,
                        false,
                        "The security-group inventory request failed"));
            }

        } catch (SdkClientException exception) {
            inventoryFailed = true;
            addError(errors, error(
                    CollectionError.ErrorCode.PROVIDER_UNAVAILABLE,
                    true,
                    "The EC2 request could not be completed"));

        } catch (RuntimeException exception) {
            inventoryFailed = true;
            addError(errors, error(
                    CollectionError.ErrorCode.INTERNAL_COLLECTION_ERROR,
                    false,
                    "Unexpected failure during EC2 security-group discovery"));
        }

        CollectionResult.Coverage coverage;

        if (inventoryFailed && assets.isEmpty()) {
            coverage = CollectionResult.Coverage.FAILED;
        } else if (stoppedEarly || !errors.isEmpty()) {
            coverage = assets.isEmpty()
                    ? CollectionResult.Coverage.FAILED
                    : CollectionResult.Coverage.PARTIAL;
        } else {
            coverage = CollectionResult.Coverage.COMPLETE;
        }

        if (coverage == CollectionResult.Coverage.FAILED
                && errors.isEmpty()) {
            addError(errors, error(
                    CollectionError.ErrorCode.PROVIDER_UNAVAILABLE,
                    true,
                    "EC2 security-group discovery could not be completed"));
        }

        return new CollectionResult(
                COLLECTOR_ID,
                COLLECTOR_VERSION,
                startedAt,
                Instant.now(),
                assets,
                coverage,
                errors);
    }

    private static NormalizedAsset toAsset(
            CloudProviderAdapter.CollectionRequest request,
            SecurityGroup group,
            String region) {

        List<Map<String, Object>> inboundRules = new ArrayList<>();
        boolean publicIngress = false;

        List<IpPermission> permissions = group.ipPermissions() == null
                ? List.of()
                : group.ipPermissions();

        for (IpPermission permission : permissions) {
            if (permission == null) {
                continue;
            }

            Integer fromPort = permission.fromPort();
            Integer toPort = permission.toPort();

            List<String> ipv4Ranges = new ArrayList<>();
            List<String> ipv6Ranges = new ArrayList<>();
            boolean publicForRule = false;

            List<IpRange> ranges = permission.ipRanges() == null
                    ? List.of()
                    : permission.ipRanges();

            for (IpRange range : ranges) {
                if (range == null || range.cidrIp() == null) {
                    continue;
                }

                ipv4Ranges.add(range.cidrIp());

                if ("0.0.0.0/0".equals(range.cidrIp())) {
                    publicForRule = true;
                }
            }

            List<Ipv6Range> ranges6 = permission.ipv6Ranges() == null
                    ? List.of()
                    : permission.ipv6Ranges();

            for (Ipv6Range range : ranges6) {
                if (range == null || range.cidrIpv6() == null) {
                    continue;
                }

                ipv6Ranges.add(range.cidrIpv6());

                if ("::/0".equals(range.cidrIpv6())) {
                    publicForRule = true;
                }
            }

            publicIngress |= publicForRule;

            Map<String, Object> rule = new HashMap<>();
            rule.put("protocol", permission.ipProtocol());
            rule.put("fromPort", fromPort);
            rule.put("toPort", toPort);
            rule.put("ipv4Ranges", ipv4Ranges);
            rule.put("ipv6Ranges", ipv6Ranges);
            rule.put("publicIngress", publicForRule);

            inboundRules.add(rule);
        }

        List<Map<String, Object>> publicIngressRules = inboundRules.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.get("publicIngress")))
                .toList();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("groupId", group.groupId());
        attributes.put("groupName", safe(group.groupName()));
        attributes.put("description", safe(group.description()));
        attributes.put("vpcId", safe(group.vpcId()));
        attributes.put("ownerId", safe(group.ownerId()));
        attributes.put("inboundRules", inboundRules);
        attributes.put("publicIngress", publicIngress);
        attributes.put("publicIngressRules", publicIngressRules);

        UUID assetId = UUID.nameUUIDFromBytes(
                (request.workspaceId() + ":AWS:EC2:SG:" + group.groupId())
                        .getBytes(StandardCharsets.UTF_8));

        return new NormalizedAsset(
                assetId,
                request.workspaceId(),
                "AWS",
                group.groupId(),
                "EC2_SECURITY_GROUP",
                safe(group.groupName()),
                region,
                publicIngress
                        ? NormalizedAsset.Exposure.PUBLIC
                        : NormalizedAsset.Exposure.UNKNOWN,
                3,
                attributes);
    }

    private static void validateRequest(
            CloudProviderAdapter.CollectionRequest request) {

        if (!COLLECTOR_ID.equals(request.collectorId())
                || !COLLECTOR_VERSION.equals(request.collectorVersion())) {
            throw new IllegalArgumentException(
                    "Unsupported EC2 security-group collector ID or version");
        }

        CloudResourceScope scope = request.scope();
        scope.validateFor(CloudProvider.AWS);

        if (scope.resourceTypes().stream().anyMatch(type ->
                !type.equalsIgnoreCase("ec2")
                        && !type.equalsIgnoreCase("ec2:security-group")
                        && !type.equalsIgnoreCase("ec2:security-group-rule"))) {
            throw new IllegalArgumentException(
                    "EC2 collector received an unsupported resource type");
        }
    }

    private static CollectionResult failedResult(
            Instant startedAt,
            CollectionError.ErrorCode code,
            boolean retryable,
            String message) {

        return new CollectionResult(
                COLLECTOR_ID,
                COLLECTOR_VERSION,
                startedAt,
                Instant.now(),
                List.of(),
                CollectionResult.Coverage.FAILED,
                List.of(error(code, retryable, message)));
    }

    private static CollectionError error(
            CollectionError.ErrorCode code,
            boolean retryable,
            String message) {

        return new CollectionError(
                COLLECTOR_ID,
                code,
                retryable,
                message);
    }

    private static void addError(
            List<CollectionError> errors,
            CollectionError error) {

        if (errors.size() < MAX_RECORDED_ERRORS) {
            errors.add(error);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void checkDeadline(long deadlineNanos) {
        if (System.nanoTime() - deadlineNanos >= 0) {
            throw new DeadlineReachedException();
        }
    }

    private static final class DeadlineReachedException
            extends RuntimeException {
    }
}
