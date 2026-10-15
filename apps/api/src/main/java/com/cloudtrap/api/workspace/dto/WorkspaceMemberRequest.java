
package com.cloudtrap.api.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WorkspaceMemberRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Pattern(
                regexp = "ADMIN|ANALYST|VIEWER",
                message = "Role must be ADMIN, ANALYST, or VIEWER"
        )
        String role
) {
}
