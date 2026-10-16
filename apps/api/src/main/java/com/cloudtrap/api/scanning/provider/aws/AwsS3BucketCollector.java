
package com.cloudtrap.api.scanning.provider.aws;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.provider.CloudProvider;
import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import com.cloudtrap.api.scanning.provider.CloudResourceScope;
import com.cloudtrap.api.scanning.provider.CollectionError;
import com.cloudtrap.api.scanning.provider.CollectionResult;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyStatusRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class AwsS3BucketCollector {

    private static final String COLLECTOR_ID = "aws-s3";
    private static final String COLLECTOR_VERSION = "1.0.0";

    private static final int MAX_RECORDED_ERRORS = 1000;
    private static final int DEFAULT_CRITICALITY = 3;

    private final S3Client s3;

    public AwsS3BucketCollector(S3Client s3) {
        this.s3 = Objects.requireNonNull(
                s3, "S3 client must not be null");
    }

    public CollectionResult collect(
            CloudProviderAdapter.CollectionRequest request) {

        Objects.requireNonNull(request, "Collection request must not be null");

        validateRequest(request);

        Instant startedAt = Instant.now();
        long timeoutNanos = request.timeout().toNanos();
        long startNanos = System.nanoTime();
        long deadlineNanos = startNanos + timeoutNanos;

        List<NormalizedAsset> assets = new ArrayList<>();
        List<CollectionError> errors = new ArrayList<>();

        boolean inventoryFailed = false;
        boolean stoppedEarly = false;
        boolean reachedAssetLimit = false;

        try {
            checkDeadline(deadlineNanos);

            ListBucketsRequest listRequest = ListBucketsRequest.builder()
                    .maxBuckets(1000)
                    .build();

            for (var page : s3.listBucketsPaginator(listRequest)) {
                checkDeadline(deadlineNanos);

                List<Bucket> buckets = page.buckets();
                if (buckets == null) {
                    addError(errors, error(
                            "inventory",
                            CollectionError.ErrorCode.INVALID_PROVIDER_RESPONSE,
                            false,
                            "AWS returned an invalid bucket inventory"));
                    inventoryFailed = true;
                    break;
                }

                for (Bucket bucket : buckets) {
                    checkDeadline(deadlineNanos);

                    if (assets.size() >= request.maximumAssets()) {
                        reachedAssetLimit = true;
                        stoppedEarly = true;
                        addError(errors, error(
                                "inventory",
                                CollectionError.ErrorCode.RESOURCE_LIMIT_EXCEEDED,
                                false,
                                "S3 discovery reached the configured asset limit"));
                        break;
                    }

                    if (bucket == null
                            || bucket.name() == null
                            || bucket.name().isBlank()) {
                        addError(errors, error(
                                "inventory",
                                CollectionError.ErrorCode.INVALID_PROVIDER_RESPONSE,
                                false,
                                "AWS returned a bucket without a valid name"));
                        continue;
                    }

                    try {
                        NormalizedAsset asset = collectBucket(
                                request, bucket.name(), deadlineNanos);

                        if (asset != null) {
                            assets.add(asset);
                        }
                    } catch (DeadlineReachedException exception) {
                        addTimeoutError(errors);
                        stoppedEarly = true;
                        break;
                    } catch (S3Exception exception) {
                        addError(errors, bucketError(
                                bucket.name(), "bucket-discovery", exception));
                    } catch (SdkClientException exception) {
                        addError(errors, error(
                                bucket.name(),
                                CollectionError.ErrorCode.PROVIDER_UNAVAILABLE,
                                true,
                                "An AWS request could not be completed"));
                    } catch (RuntimeException exception) {
                        // Do not expose exception details, request data or credentials.
                        addError(errors, error(
                                bucket.name(),
                                CollectionError.ErrorCode.INTERNAL_COLLECTION_ERROR,
                                false,
                                "Unexpected failure while inspecting an S3 bucket"));
                    }

                    if (stoppedEarly
                            || errors.size() >= MAX_RECORDED_ERRORS) {
                        break;
                    }
                }

                if (stoppedEarly
                        || errors.size() >= MAX_RECORDED_ERRORS) {
                    break;
                }
            }
        } catch (DeadlineReachedException exception) {
            addTimeoutError(errors);
            stoppedEarly = true;
        } catch (S3Exception exception) {
            inventoryFailed = true;
            addError(errors, bucketError(
                    "inventory", "list-buckets", exception));
        } catch (SdkClientException exception) {
            inventoryFailed = true;
            addError(errors, error(
                    "inventory",
                    CollectionError.ErrorCode.PROVIDER_UNAVAILABLE,
                    true,
                    "AWS bucket inventory could not be retrieved"));
        } catch (RuntimeException exception) {
            inventoryFailed = true;
            addError(errors, error(
                    "inventory",
                    CollectionError.ErrorCode.INTERNAL_COLLECTION_ERROR,
                    false,
                    "Unexpected failure while retrieving S3 inventory"));
        }

        Instant completedAt = Instant.now();

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
                    "inventory",
                    CollectionError.ErrorCode.PROVIDER_UNAVAILABLE,
                    true,
                    "S3 inventory could not be completed"));
        }

        return new CollectionResult(
                COLLECTOR_ID,
                COLLECTOR_VERSION,
                startedAt,
                completedAt,
                assets,
                coverage,
                errors
        );
    }

    private NormalizedAsset collectBucket(
            CloudProviderAdapter.CollectionRequest request,
            String bucketName,
            long deadlineNanos) {

        checkDeadline(deadlineNanos);

        String region = getBucketRegion(bucketName);

        CloudResourceScope scope = request.scope();
        if (!scope.regions().isEmpty()
                && !scope.regions().contains(region)) {
            return null;
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("bucketName", bucketName);
        attributes.put("publicAccessBlock", "UNKNOWN");
        attributes.put("bucketPolicyPublic", "UNKNOWN");
        attributes.put("encryption", "UNKNOWN");
        attributes.put("versioning", "UNKNOWN");

        boolean policyKnown = false;
        boolean policyPublic = false;
        boolean publicBlockKnown = false;
        boolean allPublicBlockFlagsEnabled = false;

        // Public access block configuration.
        checkDeadline(deadlineNanos);
        try {
            var response = s3.getPublicAccessBlock(
                    GetPublicAccessBlockRequest.builder()
                            .bucket(bucketName)
                            .build());

            var config = response.publicAccessBlockConfiguration();

            if (config != null) {
                publicBlockKnown = true;

                allPublicBlockFlagsEnabled =
                        Boolean.TRUE.equals(config.blockPublicAcls())
                        && Boolean.TRUE.equals(config.ignorePublicAcls())
                        && Boolean.TRUE.equals(config.blockPublicPolicy())
                        && Boolean.TRUE.equals(
                                config.restrictPublicBuckets());

                attributes.put(
                        "publicAccessBlock",
                        allPublicBlockFlagsEnabled
                                ? "ALL_ENABLED"
                                : "NOT_ALL_ENABLED");

                attributes.put("blockPublicAcls",
                        config.blockPublicAcls());
                attributes.put("ignorePublicAcls",
                        config.ignorePublicAcls());
                attributes.put("blockPublicPolicy",
                        config.blockPublicPolicy());
                attributes.put("restrictPublicBuckets",
                        config.restrictPublicBuckets());
            } else {
                attributes.put("publicAccessBlock", "NOT_CONFIGURED");
            }
        } catch (S3Exception exception) {
            if (isMissingPublicAccessBlock(exception)) {
                attributes.put("publicAccessBlock", "NOT_CONFIGURED");
                publicBlockKnown = true;
                allPublicBlockFlagsEnabled = false;
            } else {
                throw exception;
            }
        }

        // Bucket policy public status.
        checkDeadline(deadlineNanos);
        try {
            var response = s3.getBucketPolicyStatus(
                    GetBucketPolicyStatusRequest.builder()
                            .bucket(bucketName)
                            .build());

            if (response.policyStatus() != null
                    && response.policyStatus().isPublic() != null) {
                policyKnown = true;
                policyPublic = response.policyStatus().isPublic();

                attributes.put(
                        "bucketPolicyPublic",
                        Boolean.toString(policyPublic));
            }
        } catch (S3Exception exception) {
            // Permission errors are recorded by the outer collector.
            // Keep the asset's exposure UNKNOWN when the check fails.
            throw exception;
        }

        // Default encryption.
        checkDeadline(deadlineNanos);
        try {
            var response = s3.getBucketEncryption(
                    GetBucketEncryptionRequest.builder()
                            .bucket(bucketName)
                            .build());

            var configuration =
                    response.serverSideEncryptionConfiguration();

            boolean configured = configuration != null
                    && configuration.rules() != null
                    && !configuration.rules().isEmpty();

            attributes.put(
                    "encryption",
                    configured ? "CONFIGURED" : "UNKNOWN");

            if (configured) {
                attributes.put(
                        "encryptionRuleCount",
                        configuration.rules().size());
            }
        } catch (S3Exception exception) {
            if (isEncryptionNotConfigured(exception)) {
                attributes.put("encryption", "NOT_CONFIGURED");
            } else {
                throw exception;
            }
        }

        // Versioning.
        checkDeadline(deadlineNanos);
        try {
            var response = s3.getBucketVersioning(
                    GetBucketVersioningRequest.builder()
                            .bucket(bucketName)
                            .build());

            String status = response.statusAsString();

            attributes.put(
                    "versioning",
                    status == null || status.isBlank()
                            ? "DISABLED_OR_UNSET"
                            : status);
        } catch (S3Exception exception) {
            throw exception;
        }

        NormalizedAsset.Exposure exposure;

        if (policyKnown && policyPublic) {
            exposure = NormalizedAsset.Exposure.PUBLIC;
        } else if (policyKnown && !policyPublic
                && publicBlockKnown && allPublicBlockFlagsEnabled) {
            exposure = NormalizedAsset.Exposure.PRIVATE;
        } else {
            exposure = NormalizedAsset.Exposure.UNKNOWN;
        }

        UUID assetId = UUID.nameUUIDFromBytes(
                (request.workspaceId() + ":AWS:S3:" + bucketName)
                        .getBytes(StandardCharsets.UTF_8));

        return new NormalizedAsset(
                assetId,
                request.workspaceId(),
                "AWS",
                bucketName,
                "S3_BUCKET",
                bucketName,
                region,
                exposure,
                DEFAULT_CRITICALITY,
                attributes
        );
    }

    private String getBucketRegion(String bucketName) {
        var response = s3.getBucketLocation(
                GetBucketLocationRequest.builder()
                        .bucket(bucketName)
                        .build());

        String location = response.locationConstraintAsString();

        if (location == null
                || location.isBlank()
                || "null".equalsIgnoreCase(location)) {
            return "us-east-1";
        }

        if ("EU".equals(location)) {
            return "eu-west-1";
        }

        return location;
    }

    private static void validateRequest(
            CloudProviderAdapter.CollectionRequest request) {

        if (!COLLECTOR_ID.equals(request.collectorId())
                || !COLLECTOR_VERSION.equals(request.collectorVersion())) {
            throw new IllegalArgumentException(
                    "Unsupported S3 collector ID or version");
        }

        CloudResourceScope scope = request.scope();
        scope.validateFor(CloudProvider.AWS);

        if (scope.resourceTypes().stream().anyMatch(type ->
                !type.equalsIgnoreCase("s3")
                        && !type.equalsIgnoreCase("s3:bucket"))) {
            throw new IllegalArgumentException(
                    "S3 collector received an unsupported resource type");
        }
    }

    private static boolean isMissingPublicAccessBlock(
            S3Exception exception) {
        String code = awsErrorCode(exception);

        return "NoSuchPublicAccessBlockConfiguration".equals(code)
                || "NoSuchPublicAccessBlockConfigurationException"
                        .equals(code);
    }

    private static boolean isEncryptionNotConfigured(
            S3Exception exception) {
        String code = awsErrorCode(exception);

        return "ServerSideEncryptionConfigurationNotFoundError"
                .equals(code);
    }

    private CollectionError bucketError(
            String bucketName,
            String operation,
            S3Exception exception) {

        int status = exception.statusCode();
        String code = awsErrorCode(exception);

        if (status == 403
                || "AccessDenied".equals(code)
                || "AccessDeniedException".equals(code)) {
            return error(
                    bucketName + ":" + operation,
                    CollectionError.ErrorCode.PERMISSION_DENIED,
                    false,
                    "AWS denied the " + operation + " check for a bucket");
        }

        if (status == 429
                || status >= 500
                || "SlowDown".equals(code)
                || "RequestTimeout".equals(code)
                || "RequestTimeoutException".equals(code)) {
            return error(
                    bucketName + ":" + operation,
                    CollectionError.ErrorCode.PROVIDER_UNAVAILABLE,
                    true,
                    "AWS could not complete the " + operation + " check");
        }

        return error(
                bucketName + ":" + operation,
                CollectionError.ErrorCode.INTERNAL_COLLECTION_ERROR,
                false,
                "The " + operation + " check could not be completed");
    }

    private static String awsErrorCode(S3Exception exception) {
        return exception.awsErrorDetails() == null
                ? ""
                : exception.awsErrorDetails().errorCode();
    }

    private static void checkDeadline(long deadlineNanos) {
        if (System.nanoTime() - deadlineNanos >= 0) {
            throw new DeadlineReachedException();
        }
    }

    private static void addTimeoutError(List<CollectionError> errors) {
        addError(errors, error(
                "inventory",
                CollectionError.ErrorCode.REQUEST_TIMEOUT,
                true,
                "S3 discovery exceeded its execution deadline"));
    }


private static CollectionError error(
        String collectorId,
        CollectionError.ErrorCode code,
        boolean retryable,
        String message) {

    return new CollectionError(
            COLLECTOR_ID,
            code,
            retryable,
            message
    );
}


    private static void addError(
            List<CollectionError> errors,
            CollectionError error) {

        if (errors.size() < MAX_RECORDED_ERRORS) {
            errors.add(error);
        }
    }

    private static final class DeadlineReachedException
            extends RuntimeException {
    }
}
