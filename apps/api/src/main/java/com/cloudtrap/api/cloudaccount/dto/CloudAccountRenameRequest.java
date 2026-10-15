
package com.cloudtrap.api.cloudaccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloudAccountRenameRequest(
        @NotBlank
        @Size(max = 120)
        String displayName
) {
}
