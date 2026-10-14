
package com.cloudtrap.api.identity.service;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("postgres-test")
@TestPropertySource(
        locations = "classpath:application-postgres-test.properties"
)
class IdentityServicePostgresIntegrationTest {

    @Autowired
    private IdentityService identityService;

    @Autowired
    private UserRegistrationWriter registrationWriter;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerShouldRejectCaseInsensitiveDuplicateEmailFromDatabase()
    {
        String uniquePart = UUID.randomUUID().toString();

        String existingEmail =
                "CaseCollision-" + uniquePart + "@Example.com";

        String normalizedEmail =
                existingEmail.toLowerCase(Locale.ROOT);

        // Seed an existing account whose stored email uses mixed case.
        // This intentionally bypasses IdentityService normalization.
        User existingUser = new User(
                existingEmail,
                "integration-test-password-hash",
                "Existing User"
        );

        registrationWriter.saveNewUser(existingUser);

        // The exact lowercase lookup does not find the mixed-case row.
        // PostgreSQL's case-insensitive unique index must reject the insert.
        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> identityService.register(
                        normalizedEmail,
                        "StrongPass123",
                        "Another User"
                )
        );

        // Confirm the original record remains available.
        org.junit.jupiter.api.Assertions.assertTrue(
                userRepository.existsByEmail(existingEmail)
        );
    }
}
