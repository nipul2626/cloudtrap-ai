
package com.cloudtrap.api.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WorkspaceMemberRoleRequest(
        @NotBlank
        @Pattern(
                regexp = "ADMIN|ANALYST|VIEWER",
                message = "Role must be ADMIN, ANALYST, or VIEWER"
        )
        String role
) {
}
