package com.cloudtrap.api.workspace.controller;

import com.cloudtrap.api.workspace.dto.WorkspacePageResponse;
import com.cloudtrap.api.workspace.dto.WorkspaceRequest;
import com.cloudtrap.api.workspace.dto.WorkspaceResponse;
import com.cloudtrap.api.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(
            Authentication authentication,
            @Valid @RequestBody WorkspaceRequest request
    ) {
        WorkspaceResponse response = workspaceService.create(
                authentication.getName(),
                request.name()
        );

        return ResponseEntity
                .created(URI.create("/api/v1/workspaces/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<WorkspacePageResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0")
            @Min(0) int page,
            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(
                workspaceService.list(
                        authentication.getName(),
                        page,
                        size
                )
        );
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse> get(
            Authentication authentication,
            @PathVariable UUID workspaceId
    ) {
        return ResponseEntity.ok(
                workspaceService.get(
                        authentication.getName(),
                        workspaceId
                )
        );
    }

    @PatchMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse> rename(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceRequest request
    ) {
        return ResponseEntity.ok(
                workspaceService.rename(
                        authentication.getName(),
                        workspaceId,
                        request.name()
                )
        );
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable UUID workspaceId
    ) {
        workspaceService.delete(
                authentication.getName(),
                workspaceId
        );

        return ResponseEntity.noContent().build();
    }
}