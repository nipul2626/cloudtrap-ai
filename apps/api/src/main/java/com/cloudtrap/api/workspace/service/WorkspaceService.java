package com.cloudtrap.api.workspace.service;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import com.cloudtrap.api.workspace.dto.WorkspacePageResponse;
import com.cloudtrap.api.workspace.dto.WorkspaceResponse;
import com.cloudtrap.api.workspace.entity.Workspace;
import com.cloudtrap.api.workspace.entity.WorkspaceMembership;
import com.cloudtrap.api.workspace.repository.WorkspaceMembershipRepository;
import com.cloudtrap.api.workspace.repository.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_SLUG_BASE_LENGTH = 75;

    private static final Pattern NON_SLUG_CHARACTERS =
            Pattern.compile("[^a-z0-9]+");

    private static final Pattern REPEATED_HYPHENS =
            Pattern.compile("-{2,}");

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            UserRepository userRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create the workspace and its OWNER membership atomically.
     */
    @Transactional
    public WorkspaceResponse create(String authenticatedEmail, String name) {
        User user = requireActiveUser(authenticatedEmail);
        String normalizedName = normalizeName(name);
        String slug = generateUniqueSlug(normalizedName);

        Workspace workspace = workspaceRepository.saveAndFlush(
                new Workspace(normalizedName, slug, user.getId())
        );

        membershipRepository.saveAndFlush(
                new WorkspaceMembership(
                        workspace.getId(),
                        user.getId(),
                        "OWNER"
                )
        );

        return WorkspaceResponse.from(workspace);
    }

    /**
     * List only workspaces to which the authenticated user belongs.
     */
    public WorkspacePageResponse list(
            String authenticatedEmail,
            int page,
            int size
    ) {
        User user = requireActiveUser(authenticatedEmail);
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.ASC, "id"))
        );

        Page<WorkspaceResponse> results = workspaceRepository
                .findAllVisibleToUser(user.getId(), pageable)
                .map(WorkspaceResponse::from);

        return WorkspacePageResponse.from(results);
    }

    /**
     * Read a workspace only if the caller is a member.
     */
    public WorkspaceResponse get(
            String authenticatedEmail,
            UUID workspaceId
    ) {
        User user = requireActiveUser(authenticatedEmail);
        requireMembership(workspaceId, user.getId());

        return WorkspaceResponse.from(findWorkspace(workspaceId));
    }

    /**
     * OWNER and ADMIN can rename a workspace.
     */
    @Transactional
    public WorkspaceResponse rename(
            String authenticatedEmail,
            UUID workspaceId,
            String newName
    ) {
        User user = requireActiveUser(authenticatedEmail);
        WorkspaceMembership membership =
                requireMembership(workspaceId, user.getId());

        requireOwnerOrAdmin(membership);

        Workspace workspace = findWorkspace(workspaceId);
        workspace.rename(normalizeName(newName));

        return WorkspaceResponse.from(
                workspaceRepository.saveAndFlush(workspace)
        );
    }

    /**
     * Only OWNER can delete a workspace.
     *
     * If dependent resources such as cloud accounts prevent deletion,
     * the transaction must roll back rather than partially deleting data.
     */
    @Transactional
    public void delete(
            String authenticatedEmail,
            UUID workspaceId
    ) {
        User user = requireActiveUser(authenticatedEmail);
        WorkspaceMembership membership =
                requireMembership(workspaceId, user.getId());

        requireOwner(membership);

        Workspace workspace = findWorkspace(workspaceId);

        membershipRepository.deleteAllByWorkspaceId(workspaceId);
        membershipRepository.flush();

        workspaceRepository.delete(workspace);
        workspaceRepository.flush();
    }

    private User requireActiveUser(String authenticatedEmail) {
        if (authenticatedEmail == null
                || authenticatedEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        String normalizedEmail = authenticatedEmail
                .trim()
                .toLowerCase(Locale.ROOT);

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

    /**
     * A missing membership and an unknown workspace both return 404
     * to avoid disclosing workspaces the caller cannot access.
     */
    private WorkspaceMembership requireMembership(
            UUID workspaceId,
            UUID userId
    ) {
        if (workspaceId == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Workspace not found"
            );
        }

        return membershipRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Workspace not found"
                ));
    }

    private Workspace findWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Workspace not found"
                ));
    }

    private void requireOwnerOrAdmin(WorkspaceMembership membership) {
        String role = membership.getRole();

        if (!"OWNER".equalsIgnoreCase(role)
                && !"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to modify this workspace"
            );
        }
    }

    private void requireOwner(WorkspaceMembership membership) {
        if (!"OWNER".equalsIgnoreCase(membership.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the workspace owner can delete this workspace"
            );
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Workspace name is required"
            );
        }

        String normalized = name.trim();

        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Workspace name must not exceed 120 characters"
            );
        }

        return normalized;
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be zero or greater"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 100"
            );
        }
    }

    /**
     * Slugs are URL-safe and receive a random suffix so workspaces
     * with identical names can coexist. The database unique constraint
     * remains the final safeguard.
     */
    private String generateUniqueSlug(String name) {
        String normalized = Normalizer.normalize(
                name,
                Normalizer.Form.NFKD
        );

        String withoutMarks = normalized.replaceAll("\\p{M}+", "");

        String base = NON_SLUG_CHARACTERS
                .matcher(withoutMarks.toLowerCase(Locale.ROOT))
                .replaceAll("-");

        base = REPEATED_HYPHENS.matcher(base).replaceAll("-");
        base = base.replaceAll("^-|-$", "");

        if (base.isBlank()) {
            base = "workspace";
        }

        if (base.length() > MAX_SLUG_BASE_LENGTH) {
            base = base.substring(0, MAX_SLUG_BASE_LENGTH)
                    .replaceAll("-+$", "");
        }

        for (int attempt = 0; attempt < 5; attempt++) {
            String suffix = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8);

            String slug = base + "-" + suffix;

            if (!workspaceRepository.existsBySlug(slug)) {
                return slug;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Could not allocate a unique workspace slug"
        );
    }
}