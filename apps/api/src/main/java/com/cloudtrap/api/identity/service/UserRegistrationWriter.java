
package com.cloudtrap.api.identity.service;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class UserRegistrationWriter {

    private final UserRepository userRepository;

    public UserRegistrationWriter(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository must not be null"
        );
    }

    /**
     * Persist and flush a new user inside its own transaction.
     *
     * Flushing here makes database constraint violations surface
     * before this method returns to IdentityService.
     */
    @Transactional
    public User saveNewUser(User user) {
        Objects.requireNonNull(user, "user must not be null");

        return userRepository.saveAndFlush(user);
    }
}
