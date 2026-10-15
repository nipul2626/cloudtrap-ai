
package com.cloudtrap.api.cloudaccount.dto;

import com.cloudtrap.api.cloudaccount.entity.CloudProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CloudAccountCreateRequest(
        @NotNull
        CloudProvider provider,

        @NotBlank
        @Size(max = 255)
        String accountIdentifier,

        @NotBlank
        @Size(max = 120)
        String displayName
) {
}
