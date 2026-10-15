
package com.cloudtrap.api.identity.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        String displayName,
        Instant createdAt
) {
    public RegisterResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
