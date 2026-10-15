
package com.cloudtrap.api.cloudaccount.dto;

import com.cloudtrap.api.cloudaccount.entity.CloudAccountStatus;
import jakarta.validation.constraints.NotNull;

public record CloudAccountStatusRequest(
        @NotNull
        CloudAccountStatus status
) {
}
