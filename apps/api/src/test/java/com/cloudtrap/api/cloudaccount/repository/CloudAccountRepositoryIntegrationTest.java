
package com.cloudtrap.api.cloudaccount.repository;

import com.cloudtrap.api.cloudaccount.entity.CloudAccount;
import com.cloudtrap.api.cloudaccount.entity.CloudAccountStatus;
import com.cloudtrap.api.cloudaccount.entity.CloudProvider;
import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import com.cloudtrap.api.workspace.entity.Workspace;
import com.cloudtrap.api.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-test")
@TestPropertySource(
        locations = "classpath:application-postgres-test.properties"
)
@Transactional
class CloudAccountRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private CloudAccountRepository cloudAccountRepository;

    @Test
    void shouldPersistAndRetrieveCloudAccountWithinWorkspace() {
        Workspace workspace = createWorkspace();

        String accountIdentifier = "demo-" + UUID.randomUUID();

        CloudAccount account = new CloudAccount(
                workspace.getId(),
                CloudProvider.DEMO,
                accountIdentifier,
                "Integration Test Account"
        );

        CloudAccount saved =
                cloudAccountRepository.saveAndFlush(account);

        var result = cloudAccountRepository.findByIdAndWorkspaceId(
                saved.getId(),
                workspace.getId()
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getProvider())
                .isEqualTo(CloudProvider.DEMO);
        assertThat(result.orElseThrow().getStatus())
                .isEqualTo(CloudAccountStatus.ACTIVE);
        assertThat(result.orElseThrow().getAccountIdentifier())
                .isEqualTo(accountIdentifier);
    }

    @Test
    void shouldFilterCloudAccountsByWorkspaceAndProvider() {
        Workspace workspace = createWorkspace();

        cloudAccountRepository.saveAndFlush(
                new CloudAccount(
                        workspace.getId(),
                        CloudProvider.DEMO,
                        "demo-" + UUID.randomUUID(),
                        "Demo Account"
                )
        );

        cloudAccountRepository.saveAndFlush(
                new CloudAccount(
                        workspace.getId(),
                        CloudProvider.AWS,
                        "aws-" + UUID.randomUUID(),
                        "AWS Account"
                )
        );

        Page<CloudAccount> results =
                cloudAccountRepository.findAllByWorkspaceIdAndProvider(
                        workspace.getId(),
                        CloudProvider.DEMO,
                        PageRequest.of(0, 10)
                );

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().getFirst().getProvider())
                .isEqualTo(CloudProvider.DEMO);
    }

    @Test
    void shouldFindDisabledAccountsSeparately() {
        Workspace workspace = createWorkspace();

        CloudAccount account = cloudAccountRepository.saveAndFlush(
                new CloudAccount(
                        workspace.getId(),
                        CloudProvider.DEMO,
                        "disabled-" + UUID.randomUUID(),
                        "Disabled Test Account"
                )
        );

        account.disable();
        cloudAccountRepository.saveAndFlush(account);

        Page<CloudAccount> disabledAccounts =
                cloudAccountRepository.findAllByWorkspaceIdAndStatus(
                        workspace.getId(),
                        CloudAccountStatus.DISABLED,
                        PageRequest.of(0, 10)
                );

        assertThat(disabledAccounts.getContent())
                .extracting(CloudAccount::getId)
                .contains(account.getId());
    }

    private Workspace createWorkspace() {
        User user = userRepository.saveAndFlush(
                new User(
                        "cloud-account-test-" + UUID.randomUUID()
                                + "@example.com",
                        "test-password-hash",
                        "Cloud Account Test User"
                )
        );

        return workspaceRepository.saveAndFlush(
                new Workspace(
                        "Cloud Account Test Workspace",
                        "cloud-account-" + UUID.randomUUID(),
                        user.getId()
                )
        );
    }
}
