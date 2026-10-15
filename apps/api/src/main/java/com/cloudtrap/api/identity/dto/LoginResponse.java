
package com.cloudtrap.api.identity.dto;

public record LoginResponse(
        boolean authenticated,
        String email
) {
}
