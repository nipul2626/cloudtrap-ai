
package com.cloudtrap.api.workspace.controller;

import com.cloudtrap.api.workspace.dto.WorkspaceMemberRequest;
import com.cloudtrap.api.workspace.dto.WorkspaceMemberResponse;
import com.cloudtrap.api.workspace.dto.WorkspaceMemberRoleRequest;
import com.cloudtrap.api.workspace.service.WorkspaceMemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberService memberService;

    public WorkspaceMemberController(
            WorkspaceMemberService memberService
    ) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceMemberResponse>> listMembers(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(
                memberService.listMembers(
                        authentication.getName(),
                        workspaceId,
                        page,
                        size
                )
        );
    }

    @PostMapping
    public ResponseEntity<WorkspaceMemberResponse> addMember(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceMemberRequest request
    ) {
        return ResponseEntity.status(201).body(
                memberService.addMember(
                        authentication.getName(),
                        workspaceId,
                        request.email(),
                        request.role()
                )
        );
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<WorkspaceMemberResponse> changeRole(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @Valid @RequestBody WorkspaceMemberRoleRequest request
    ) {
        return ResponseEntity.ok(
                memberService.changeRole(
                        authentication.getName(),
                        workspaceId,
                        userId,
                        request.role()
                )
        );
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId
    ) {
        memberService.removeMember(
                authentication.getName(),
                workspaceId,
                userId
        );

        return ResponseEntity.noContent().build();
    }
}
