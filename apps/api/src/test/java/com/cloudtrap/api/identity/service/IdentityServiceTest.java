
package com.cloudtrap.api.identity.service;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRegistrationWriter registrationWriter;

    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        identityService = new IdentityService(
                userRepository,
                passwordEncoder,
                registrationWriter
        );
    }

    @Test
    void registerShouldNormalizeEmailHashPasswordAndSaveUser() {
        String email = "  USER@Example.COM  ";
        String rawPassword = "StrongPass123";
        String passwordHash = "$2a$12$exampleEncodedPasswordHash";
        String displayName = "  Test User  ";

        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(rawPassword))
                .thenReturn(passwordHash);

        when(registrationWriter.saveNewUser(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        identityService.register(email, rawPassword, displayName);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(registrationWriter).saveNewUser(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("user@example.com", savedUser.getEmail());
        assertEquals(passwordHash, savedUser.getPasswordHash());
        assertEquals("Test User", savedUser.getDisplayName());
        assertTrue(savedUser.isActive());

        verify(userRepository).existsByEmail("user@example.com");
        verify(passwordEncoder).encode(rawPassword);

        verifyNoMoreInteractions(userRepository, passwordEncoder);
        verifyNoMoreInteractions(registrationWriter);
    }

    @Test
    void registerShouldRejectDuplicateEmailWithoutHashingOrSaving() {
        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> identityService.register(
                        "USER@example.com",
                        "StrongPass123",
                        "Test User"
                )
        );

        verify(userRepository).existsByEmail("user@example.com");
        verifyNoMoreInteractions(userRepository);

        verifyNoInteractions(passwordEncoder, registrationWriter);
    }

    @Test
    void registerShouldRejectBlankEmailBeforeAccessingDependencies() {
        assertThrows(
                IllegalArgumentException.class,
                () -> identityService.register(
                        "   ",
                        "StrongPass123",
                        "Test User"
                )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                registrationWriter
        );
    }

    @Test
    void registerShouldRejectPasswordShorterThanTwelveCodePoints() {
        assertThrows(
                IllegalArgumentException.class,
                () -> identityService.register(
                        "user@example.com",
                        "Short123",
                        "Test User"
                )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                registrationWriter
        );
    }

    @Test
    void registerShouldRejectPasswordLongerThanBcryptByteLimit() {
        String oversizedPassword = "a".repeat(73);

        assertThrows(
                IllegalArgumentException.class,
                () -> identityService.register(
                        "user@example.com",
                        oversizedPassword,
                        "Test User"
                )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                registrationWriter
        );
    }

    @Test
    void registerShouldRejectBlankDisplayNameBeforeDatabaseAccess() {
        assertThrows(
                IllegalArgumentException.class,
                () -> identityService.register(
                        "user@example.com",
                        "StrongPass123",
                        "   "
                )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                registrationWriter
        );
    }


@Test
void registerShouldTranslateDatabaseEmailUniquenessViolation() {
    when(userRepository.existsByEmail("user@example.com"))
            .thenReturn(false);
    when(passwordEncoder.encode("StrongPass123"))
            .thenReturn("$2a$12$exampleEncodedPasswordHash");

    ConstraintViolationException databaseException =
            new ConstraintViolationException(
                    "Duplicate email",
                    new SQLException("Duplicate key", "23505"),
                    "uq_users_email_lower"
            );

    DataIntegrityViolationException persistenceException =
            new DataIntegrityViolationException(
                    "Email uniqueness violation",
                    databaseException
            );

    when(registrationWriter.saveNewUser(any(User.class)))
            .thenThrow(persistenceException);

    assertThrows(
            EmailAlreadyRegisteredException.class,
            () -> identityService.register(
                    "user@example.com",
                    "StrongPass123",
                    "Test User"
            )
    );

    verify(userRepository).existsByEmail("user@example.com");
    verify(passwordEncoder).encode("StrongPass123");
    verify(registrationWriter).saveNewUser(any(User.class));
}

@Test
void registerShouldPropagateUnrelatedDatabaseIntegrityViolation() {
    when(userRepository.existsByEmail("user@example.com"))
            .thenReturn(false);
    when(passwordEncoder.encode("StrongPass123"))
            .thenReturn("$2a$12$exampleEncodedPasswordHash");

    ConstraintViolationException databaseException =
            new ConstraintViolationException(
                    "Another constraint failed",
                    new SQLException("Constraint violation", "23505"),
                    "some_other_constraint"
            );

    DataIntegrityViolationException persistenceException =
            new DataIntegrityViolationException(
                    "Unrelated database constraint violation",
                    databaseException
            );

    when(registrationWriter.saveNewUser(any(User.class)))
            .thenThrow(persistenceException);

    DataIntegrityViolationException thrown = assertThrows(
            DataIntegrityViolationException.class,
            () -> identityService.register(
                    "user@example.com",
                    "StrongPass123",
                    "Test User"
            )
    );

    assertEquals(persistenceException, thrown);

    verify(userRepository).existsByEmail("user@example.com");
    verify(passwordEncoder).encode("StrongPass123");
    verify(registrationWriter).saveNewUser(any(User.class));
}


    @Test
    void registerShouldRejectNullPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> identityService.register(
                        "user@example.com",
                        null,
                        "Test User"
                )
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                registrationWriter
        );
    }

@Test
void registerShouldRejectMalformedEmailBeforeAccessingDependencies() {
    assertThrows(
            IllegalArgumentException.class,
            () -> identityService.register(
                    "not-an-email",
                    "StrongPass123",
                    "Test User"
            )
    );

    verifyNoInteractions(userRepository, passwordEncoder, registrationWriter);
}

@Test
void registerShouldRejectEmailWithoutDomainSuffix() {
    assertThrows(
            IllegalArgumentException.class,
            () -> identityService.register(
                    "user@localhost",
                    "StrongPass123",
                    "Test User"
            )
    );

    verifyNoInteractions(userRepository, passwordEncoder, registrationWriter);
}

@Test
void registerShouldRejectEmailWithConsecutiveDots() {
    assertThrows(
            IllegalArgumentException.class,
            () -> identityService.register(
                    "first..last@example.com",
                    "StrongPass123",
                    "Test User"
            )
    );

    verifyNoInteractions(userRepository, passwordEncoder, registrationWriter);
}


    @Test
    void registerShouldRejectDuplicateEmailWithSpecificException() {
        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(true);

        EmailAlreadyRegisteredException exception = assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> identityService.register(
                        "user@example.com",
                        "StrongPass123",
                        "Test User"
                )
        );

        assertEquals(
                "An account with this email address already exists",
                exception.getMessage()
        );

        verify(userRepository).existsByEmail("user@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder, registrationWriter);
    }
}
