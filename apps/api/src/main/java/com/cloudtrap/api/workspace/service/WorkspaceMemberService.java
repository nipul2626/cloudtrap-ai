
package com.cloudtrap.api.workspace.service;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import com.cloudtrap.api.workspace.dto.WorkspaceMemberResponse;
import com.cloudtrap.api.workspace.entity.WorkspaceMembership;
import com.cloudtrap.api.workspace.repository.WorkspaceMembershipRepository;
import com.cloudtrap.api.workspace.repository.WorkspaceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WorkspaceMemberService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public WorkspaceMemberService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            UserRepository userRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public List<WorkspaceMemberResponse> listMembers(
            String authenticatedEmail,
            UUID workspaceId,
            int page,
            int size
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        requireWorkspaceExists(workspaceId);
        requireMembership(workspaceId, actor.getId());

        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination parameters"
            );
        }

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "createdAt")
                        .and(Sort.by(Sort.Direction.ASC, "id"))
        );

        return membershipRepository.findAllByWorkspaceId(
                        workspaceId, pageable
                )
                .map(membership -> {
                    User member = userRepository.findById(
                                    membership.getUserId()
                            )
                            .orElseThrow(() -> new IllegalStateException(
                                    "Workspace membership references a missing user"
                            ));

                    return WorkspaceMemberResponse.from(
                            membership, member
                    );
                })
                .getContent();
    }

    @Transactional
    public WorkspaceMemberResponse addMember(
            String authenticatedEmail,
            UUID workspaceId,
            String email,
            String requestedRole
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        WorkspaceMembership actorMembership =
                requireMembership(workspaceId, actor.getId());

        requireAdminOrOwner(actorMembership);

        String normalizedEmail = normalizeEmail(email);
        String role = normalizeAssignableRole(requestedRole);

        User target = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Registered user not found"
                ));

        if (!target.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inactive users cannot be added to a workspace"
            );
        }

        if (membershipRepository.existsByWorkspaceIdAndUserId(
                workspaceId, target.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User is already a member of this workspace"
            );
        }

        /*
         * ADMINs cannot create other ADMINs. Only an OWNER can
         * assign the ADMIN role.
         */
        if ("ADMIN".equals(role)
                && !"OWNER".equals(actorMembership.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the owner can assign the ADMIN role"
            );
        }

        try {
            WorkspaceMembership membership =
                    membershipRepository.saveAndFlush(
                            new WorkspaceMembership(
                                    workspaceId,
                                    target.getId(),
                                    role
                            )
                    );

            return WorkspaceMemberResponse.from(membership, target);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Membership conflicts with an existing record"
            );
        }
    }

    @Transactional
    public WorkspaceMemberResponse changeRole(
            String authenticatedEmail,
            UUID workspaceId,
            UUID targetUserId,
            String requestedRole
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        WorkspaceMembership actorMembership =
                requireMembership(workspaceId, actor.getId());

        requireAdminOrOwner(actorMembership);

        WorkspaceMembership targetMembership =
                requireMembership(workspaceId, targetUserId);

        String newRole = normalizeAssignableRole(requestedRole);
        String oldRole = targetMembership.getRole();

        if ("OWNER".equals(oldRole)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Workspace ownership cannot be changed through this endpoint"
            );
        }

        if ("ADMIN".equals(newRole)
                && !"OWNER".equals(actorMembership.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the owner can assign the ADMIN role"
            );
        }

        if (actor.getId().equals(targetUserId)
                && "OWNER".equals(actorMembership.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Use a dedicated ownership transfer operation"
            );
        }

        targetMembership.changeRole(newRole);

        WorkspaceMembership saved =
                membershipRepository.saveAndFlush(targetMembership);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "Workspace membership references a missing user"
                ));

        return WorkspaceMemberResponse.from(saved, target);
    }

    @Transactional
    public void removeMember(
            String authenticatedEmail,
            UUID workspaceId,
            UUID targetUserId
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        WorkspaceMembership actorMembership =
                requireMembership(workspaceId, actor.getId());

        requireAdminOrOwner(actorMembership);

        WorkspaceMembership targetMembership =
                requireMembership(workspaceId, targetUserId);

        if ("OWNER".equals(targetMembership.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The workspace owner cannot be removed"
            );
        }

        if ("ADMIN".equals(actorMembership.getRole())
                && "ADMIN".equals(targetMembership.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admins cannot remove other admins"
            );
        }

        membershipRepository.delete(targetMembership);
        membershipRepository.flush();
    }

    private User requireActiveUser(String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found"
                ));

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is inactive"
            );
        }

        return user;
    }

    private WorkspaceMembership requireMembership(
            UUID workspaceId,
            UUID userId
    ) {
        if (workspaceId == null || userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Workspace or membership not found"
            );
        }

        return membershipRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Workspace or membership not found"
                ));
    }

    private void requireWorkspaceExists(UUID workspaceId) {
        if (workspaceId == null
                || !workspaceRepository.existsById(workspaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Workspace not found"
            );
        }
    }

    private void requireAdminOrOwner(
            WorkspaceMembership membership
    ) {
        if (!"OWNER".equals(membership.getRole())
                && !"ADMIN".equals(membership.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to manage workspace members"
            );
        }
    }

    private String normalizeAssignableRole(String role) {
        if (role == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role is required"
            );
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);

        if (!normalized.equals("ADMIN")
                && !normalized.equals("ANALYST")
                && !normalized.equals("VIEWER")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be ADMIN, ANALYST, or VIEWER"
            );
        }

        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
