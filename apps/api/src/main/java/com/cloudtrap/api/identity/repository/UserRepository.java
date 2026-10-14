
package com.cloudtrap.api.identity.repository;

import com.cloudtrap.api.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for persisting and querying users.
 *
 * Contains data-access operations only.
 * Authentication and business rules belong in the service layer.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their normalized email address.
     *
     * Callers must apply the same email-normalization policy
     * during registration and authentication.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether an email address is already registered.
     *
     * The database unique constraint remains the final safeguard
     * against concurrent duplicate registrations.
     */
    boolean existsByEmail(String email);
}
