
package com.cloudtrap.api.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email address is invalid")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, message = "Password must contain at least 12 characters")
        String password,

        @NotBlank(message = "Display name is required")
        @Size(max = 120, message = "Display name must not exceed 120 characters")
        String displayName

) {
}
