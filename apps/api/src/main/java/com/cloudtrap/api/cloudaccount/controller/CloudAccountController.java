
package com.cloudtrap.api.cloudaccount.controller;

import com.cloudtrap.api.cloudaccount.dto.CloudAccountCreateRequest;
import com.cloudtrap.api.cloudaccount.dto.CloudAccountRenameRequest;
import com.cloudtrap.api.cloudaccount.dto.CloudAccountResponse;
import com.cloudtrap.api.cloudaccount.dto.CloudAccountStatusRequest;
import com.cloudtrap.api.cloudaccount.entity.CloudAccountStatus;
import com.cloudtrap.api.cloudaccount.entity.CloudProvider;
import com.cloudtrap.api.cloudaccount.service.CloudAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/workspaces/{workspaceId}/cloud-accounts")
public class CloudAccountController {

    private final CloudAccountService cloudAccountService;

    public CloudAccountController(
            CloudAccountService cloudAccountService
    ) {
        this.cloudAccountService = cloudAccountService;
    }

    @PostMapping
    public ResponseEntity<CloudAccountResponse> create(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CloudAccountCreateRequest request
    ) {
        CloudAccountResponse response = cloudAccountService.create(
                authentication.getName(), workspaceId, request
        );

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CloudAccountResponse>> list(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) CloudProvider provider,
            @RequestParam(required = false) CloudAccountStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(
                cloudAccountService.list(
                        authentication.getName(),
                        workspaceId,
                        provider,
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<CloudAccountResponse> get(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(
                cloudAccountService.get(
                        authentication.getName(),
                        workspaceId,
                        accountId
                )
        );
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<CloudAccountResponse> rename(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID accountId,
            @Valid @RequestBody CloudAccountRenameRequest request
    ) {
        return ResponseEntity.ok(
                cloudAccountService.rename(
                        authentication.getName(),
                        workspaceId,
                        accountId,
                        request.displayName()
                )
        );
    }

    @PatchMapping("/{accountId}/status")
    public ResponseEntity<CloudAccountResponse> changeStatus(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID accountId,
            @Valid @RequestBody CloudAccountStatusRequest request
    ) {
        return ResponseEntity.ok(
                cloudAccountService.changeStatus(
                        authentication.getName(),
                        workspaceId,
                        accountId,
                        request.status()
                )
        );
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID accountId
    ) {
        cloudAccountService.delete(
                authentication.getName(), workspaceId, accountId
        );

        return ResponseEntity.noContent().build();
    }
}
