
package com.cloudtrap.api.scanning.provider.aws;

import com.cloudtrap.api.scanning.domain.NormalizedAsset;
import com.cloudtrap.api.scanning.provider.CloudProvider;
import com.cloudtrap.api.scanning.provider.CloudProviderAdapter;
import com.cloudtrap.api.scanning.provider.CloudResourceScope;
import com.cloudtrap.api.scanning.provider.CollectionError;
import com.cloudtrap.api.scanning.provider.CollectionResult;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import software.amazon.awssdk.services.sts.model.StsException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class AwsStsCloudProviderAdapter implements CloudProviderAdapter {

    private final StsClient stsClient;

    public AwsStsCloudProviderAdapter(StsClient stsClient) {
        this.stsClient = Objects.requireNonNull(
                stsClient, "STS client must not be null");
    }

    @Override
    public CloudProvider provider() {
        return CloudProvider.AWS;
    }

    @Override
    public AccountValidation validateAccount(CloudResourceScope scope)
            throws ProviderAdapterException {

        Objects.requireNonNull(scope, "Scope must not be null");
        scope.validateFor(CloudProvider.AWS);

        final GetCallerIdentityResponse identity;

        try {
            identity = stsClient.getCallerIdentity();
        } catch (StsException exception) {
            int status = exception.statusCode();
            String errorCode = exception.awsErrorDetails() == null
                    ? ""
                    : exception.awsErrorDetails().errorCode();

            if (status == 403 || "AccessDenied".equals(errorCode)
                    || "AccessDeniedException".equals(errorCode)) {
                throw new ProviderAdapterException(
                        CollectionError.ErrorCode.PERMISSION_DENIED,
                        false,
                        "AWS denied the identity validation request");
            }

            boolean retryable = status == 429 || status >= 500;
            throw new ProviderAdapterException(
                    retryable
                            ? CollectionError.ErrorCode.PROVIDER_UNAVAILABLE
                            : CollectionError.ErrorCode.INTERNAL_COLLECTION_ERROR,
                    retryable,
                    "AWS identity validation failed");
        } catch (SdkClientException exception) {
            throw new ProviderAdapterException(
                    CollectionError.ErrorCode.PROVIDER_UNAVAILABLE,
                    true,
                    "Could not reach AWS STS or resolve AWS credentials");
        }

        if (identity == null || identity.account() == null
                || identity.account().isBlank()) {
            throw new ProviderAdapterException(
                    CollectionError.ErrorCode.INVALID_PROVIDER_RESPONSE,
                    false,
                    "AWS STS returned an incomplete identity response");
        }

        String actualAccountId = identity.account();

        if (!scope.accountId().equals(actualAccountId)) {
            return new AccountValidation(
                    CloudProvider.AWS,
                    scope.accountId(),
                    false,
                    "Authenticated AWS account does not match the requested account");
        }

        return new AccountValidation(
                CloudProvider.AWS,
                actualAccountId,
                true,
                "AWS account identity validated");
    }

    @Override
    public CollectionResult collect(CollectionRequest request) {
        Objects.requireNonNull(request, "Collection request must not be null");

        Instant startedAt = Instant.now();
        Instant completedAt = Instant.now();

        CollectionError error = new CollectionError(
                request.collectorId(),
                CollectionError.ErrorCode.UNSUPPORTED_RESOURCE_TYPE,
                false,
                "AWS resource collection is not implemented yet");

        return new CollectionResult(
                request.collectorId(),
                request.collectorVersion(),
                startedAt,
                completedAt,
                List.<NormalizedAsset>of(),
                CollectionResult.Coverage.FAILED,
                List.of(error)
        );
    }
}
