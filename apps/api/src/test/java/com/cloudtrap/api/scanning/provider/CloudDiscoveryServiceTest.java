
package com.cloudtrap.api.scanning.provider;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CloudDiscoveryServiceTest {

    private static final String ACCOUNT_ID = "123456789012";
    private static final String COLLECTOR_ID = "aws-s3";
    private static final String COLLECTOR_VERSION = "1.0.0";
    private static final UUID WORKSPACE_ID =
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    @Test
    void validatesAccountBeforeCollecting() throws Exception {
        AtomicBoolean collectionCalled = new AtomicBoolean(false);

        CloudProviderAdapter adapter = new TestAdapter(true, collectionCalled);
        CloudDiscoveryService service = new CloudDiscoveryService(
                new CloudProviderAdapterRegistry(List.of(adapter)));

        CloudDiscoveryService.DiscoveryOutcome outcome =
                service.discover("aws", request());

        assertEquals(CloudProvider.AWS, outcome.provider());
        assertTrue(outcome.accountValidation().valid());
        assertEquals(
                CollectionResult.Coverage.COMPLETE,
                outcome.collectionResult().coverage());
        assertTrue(collectionCalled.get());
    }

    @Test
    void stopsWhenAccountValidationFails() {
        AtomicBoolean collectionCalled = new AtomicBoolean(false);

        CloudProviderAdapter adapter = new TestAdapter(false, collectionCalled);
        CloudDiscoveryService service = new CloudDiscoveryService(
                new CloudProviderAdapterRegistry(List.of(adapter)));

        assertThrows(
                CloudDiscoveryService.AccountValidationFailedException.class,
                () -> service.discover("aws", request()));

        assertFalse(collectionCalled.get());
    }

    @Test
    void rejectsUnsupportedProviderIdentifier() {
        CloudDiscoveryService service = new CloudDiscoveryService(
                new CloudProviderAdapterRegistry(List.of()));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.discover("demo", request()));
    }

    private static CloudProviderAdapter.CollectionRequest request() {
        CloudResourceScope scope = new CloudResourceScope(
                ACCOUNT_ID,
                List.of("us-east-1"),
                List.of("s3")
        );

        return new CloudProviderAdapter.CollectionRequest(
                WORKSPACE_ID,
                scope,
                COLLECTOR_ID,
                COLLECTOR_VERSION,
                Duration.ofSeconds(20),
                100
        );
    }

    private static final class TestAdapter implements CloudProviderAdapter {

        private final boolean accountValid;
        private final AtomicBoolean collectionCalled;

        private TestAdapter(
                boolean accountValid, AtomicBoolean collectionCalled) {
            this.accountValid = accountValid;
            this.collectionCalled = collectionCalled;
        }

        @Override
        public CloudProvider provider() {
            return CloudProvider.AWS;
        }

        @Override
        public AccountValidation validateAccount(CloudResourceScope scope) {
            return new AccountValidation(
                    CloudProvider.AWS,
                    scope.accountId(),
                    accountValid,
                    accountValid ? "Account validated" : "Account access denied"
            );
        }

        @Override
        public CollectionResult collect(CollectionRequest request) {
            collectionCalled.set(true);

            Instant started = Instant.now();
            Instant completed = started.plusMillis(1);

            return new CollectionResult(
                    request.collectorId(),
                    request.collectorVersion(),
                    started,
                    completed,
                    List.of(),
                    CollectionResult.Coverage.COMPLETE,
                    List.of()
            );
        }
    }
}
