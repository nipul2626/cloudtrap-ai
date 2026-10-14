
package com.cloudtrap.api.identity.service;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class IdentityService {

    private static final String EMAIL_UNIQUE_CONSTRAINT =
            "uq_users_email_lower";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRegistrationWriter registrationWriter;

    public IdentityService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserRegistrationWriter registrationWriter
    ) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository must not be null"
        );
        this.passwordEncoder = Objects.requireNonNull(
                passwordEncoder,
                "passwordEncoder must not be null"
        );
        this.registrationWriter = Objects.requireNonNull(
                registrationWriter,
                "registrationWriter must not be null"
        );
    }

    public User register(
            String email,
            String rawPassword,
            String displayName
    ) {
        String normalizedEmail =
                RegistrationValidator.normalizeEmail(email);

        RegistrationValidator.validatePassword(rawPassword);

        String normalizedDisplayName =
                RegistrationValidator.validateDisplayName(displayName);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = new User(
                normalizedEmail,
                passwordHash,
                normalizedDisplayName
        );

        try {
            return registrationWriter.saveNewUser(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniquenessViolation(exception)) {
                throw new EmailAlreadyRegisteredException();
            }

            // Do not misreport unrelated database failures as duplicates.
            throw exception;
        }
    }

    private boolean isEmailUniquenessViolation(Throwable failure) {
        Throwable current = failure;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return EMAIL_UNIQUE_CONSTRAINT.equals(
                        violation.getConstraintName()
                );
            }

            if (current.getCause() == current) {
                break;
            }

            current = current.getCause();
        }

        return false;
    }
}
