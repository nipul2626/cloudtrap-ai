
package com.cloudtrap.api.workspace.repository;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import com.cloudtrap.api.workspace.entity.Workspace;
import com.cloudtrap.api.workspace.entity.WorkspaceMembership;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class WorkspaceRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository membershipRepository;

    @Test
    void shouldPersistAndRetrieveWorkspaceBySlug() {
        User user = userRepository.saveAndFlush(
                new User(
                        "workspace-test-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash",
                        "Workspace Test User"
                )
        );

        String slug = "workspace-" + UUID.randomUUID();

        Workspace workspace = new Workspace(
                "Integration Test Workspace",
                slug,
                user.getId()
        );

        Workspace savedWorkspace =
                workspaceRepository.saveAndFlush(workspace);

        var result = workspaceRepository.findBySlug(slug);

        assertThat(savedWorkspace.getId()).isNotNull();
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getName())
                .isEqualTo("Integration Test Workspace");
        assertThat(result.orElseThrow().getCreatedBy())
                .isEqualTo(user.getId());
        assertThat(workspaceRepository.existsBySlug(slug)).isTrue();
    }

    @Test
    void shouldPersistWorkspaceOwnerMembership() {
        User user = userRepository.saveAndFlush(
                new User(
                        "membership-test-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash",
                        "Membership Test User"
                )
        );

        Workspace workspace = workspaceRepository.saveAndFlush(
                new Workspace(
                        "Membership Test Workspace",
                        "membership-" + UUID.randomUUID(),
                        user.getId()
                )
        );

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace.getId(),
                user.getId(),
                "OWNER"
        );

        WorkspaceMembership savedMembership =
                membershipRepository.saveAndFlush(membership);

        var result = membershipRepository
                .findByWorkspaceIdAndUserId(
                        workspace.getId(),
                        user.getId()
                );

        assertThat(savedMembership.getId()).isNotNull();
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getRole()).isEqualTo("OWNER");
        assertThat(membershipRepository.countByWorkspaceId(workspace.getId()))
                .isEqualTo(1);
    }
}
