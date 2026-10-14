
package com.cloudtrap.api.identity.repository;

import com.cloudtrap.api.identity.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-test")
@TestPropertySource(
        locations = "classpath:application-postgres-test.properties"
)
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistAndRetrieveUserByEmail() {
        String email = "repository-test-" + UUID.randomUUID()
                + "@example.com";

        User user = new User(
                email,
                "test-password-hash",
                "Repository Test User"
        );

        User savedUser = userRepository.saveAndFlush(user);

        Optional<User> result = userRepository.findByEmail(email);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getEmail()).isEqualTo(email);
        assertThat(result.orElseThrow().getDisplayName())
                .isEqualTo("Repository Test User");
        assertThat(userRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        String email = "missing-" + UUID.randomUUID() + "@example.com";

        assertThat(userRepository.findByEmail(email)).isEmpty();
        assertThat(userRepository.existsByEmail(email)).isFalse();
    }
}
