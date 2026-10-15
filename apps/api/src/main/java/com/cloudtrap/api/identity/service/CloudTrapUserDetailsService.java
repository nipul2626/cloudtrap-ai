
package com.cloudtrap.api.identity.service;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloudTrapUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CloudTrapUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        if (email == null || email.isBlank()) {
            throw invalidCredentials();
        }

        final String normalizedEmail;

        try {
            normalizedEmail = RegistrationValidator.normalizeEmail(email);
        } catch (IllegalArgumentException exception) {
            throw invalidCredentials();
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(
                        CloudTrapUserDetailsService::invalidCredentials
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.isActive())
                .authorities("ROLE_USER")
                .build();
    }

    private static UsernameNotFoundException invalidCredentials() {
        return new UsernameNotFoundException(
                "Invalid email or password"
        );
    }
}
