
package com.cloudtrap.api.cloudaccount.service;

import com.cloudtrap.api.cloudaccount.dto.CloudAccountCreateRequest;
import com.cloudtrap.api.cloudaccount.dto.CloudAccountResponse;
import com.cloudtrap.api.cloudaccount.entity.CloudAccount;
import com.cloudtrap.api.cloudaccount.entity.CloudAccountStatus;
import com.cloudtrap.api.cloudaccount.entity.CloudProvider;
import com.cloudtrap.api.cloudaccount.repository.CloudAccountRepository;
import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import com.cloudtrap.api.workspace.entity.WorkspaceMembership;
import com.cloudtrap.api.workspace.repository.WorkspaceMembershipRepository;
import com.cloudtrap.api.workspace.repository.WorkspaceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CloudAccountService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CloudAccountRepository accountRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public CloudAccountService(
            CloudAccountRepository accountRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            UserRepository userRepository
    ) {
        this.accountRepository = accountRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CloudAccountResponse create(
            String authenticatedEmail,
            UUID workspaceId,
            CloudAccountCreateRequest request
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        requireWorkspace(workspaceId);
        requireWriter(workspaceId, actor.getId());

        String identifier = normalize(
                request.accountIdentifier(), "Account identifier", 255
        );
        String displayName = normalize(
                request.displayName(), "Display name", 120
        );
        CloudProvider provider = request.provider();

        if (accountRepository
                .existsByWorkspaceIdAndProviderAndAccountIdentifier(
                        workspaceId, provider, identifier
                )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This provider account is already registered"
            );
        }

        try {
            CloudAccount account = accountRepository.saveAndFlush(
                    new CloudAccount(
                            workspaceId,
                            provider,
                            identifier,
                            displayName
                    )
            );

            return CloudAccountResponse.from(account);
        } catch (DataIntegrityViolationException exception) {
            // The unique database constraint handles concurrent requests.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This provider account is already registered"
            );
        }
    }

    public Page<CloudAccountResponse> list(
            String authenticatedEmail,
            UUID workspaceId,
            CloudProvider provider,
            CloudAccountStatus status,
            int page,
            int size
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        requireWorkspace(workspaceId);
        requireMembership(workspaceId, actor.getId());
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.ASC, "id"))
        );

        Page<CloudAccount> results;

        if (provider != null && status != null) {
            results = accountRepository
                    .findAllByWorkspaceIdAndProviderAndStatus(
                            workspaceId, provider, status, pageable
                    );
        } else if (provider != null) {
            results = accountRepository.findAllByWorkspaceIdAndProvider(
                    workspaceId, provider, pageable
            );
        } else if (status != null) {
            results = accountRepository.findAllByWorkspaceIdAndStatus(
                    workspaceId, status, pageable
            );
        } else {
            results = accountRepository.findAllByWorkspaceId(
                    workspaceId, pageable
            );
        }

        return results.map(CloudAccountResponse::from);
    }

    public CloudAccountResponse get(
            String authenticatedEmail,
            UUID workspaceId,
            UUID accountId
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        requireMembership(workspaceId, actor.getId());

        return CloudAccountResponse.from(
                findAccount(workspaceId, accountId)
        );
    }

    @Transactional
    public CloudAccountResponse rename(
            String authenticatedEmail,
            UUID workspaceId,
            UUID accountId,
            String displayName
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        requireWriter(workspaceId, actor.getId());

        CloudAccount account = findAccount(workspaceId, accountId);
        account.rename(normalize(displayName, "Display name", 120));

        return CloudAccountResponse.from(
                accountRepository.saveAndFlush(account)
        );
    }

    @Transactional
    public CloudAccountResponse changeStatus(
            String authenticatedEmail,
            UUID workspaceId,
            UUID accountId,
            CloudAccountStatus status
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        requireWriter(workspaceId, actor.getId());

        CloudAccount account = findAccount(workspaceId, accountId);

        if (status == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account status is required"
            );
        }

        if (status == CloudAccountStatus.ACTIVE) {
            account.enable();
        } else {
            account.disable();
        }

        return CloudAccountResponse.from(
                accountRepository.saveAndFlush(account)
        );
    }

    @Transactional
    public void delete(
            String authenticatedEmail,
            UUID workspaceId,
            UUID accountId
    ) {
        User actor = requireActiveUser(authenticatedEmail);
        requireWriter(workspaceId, actor.getId());

        CloudAccount account = findAccount(workspaceId, accountId);
        accountRepository.delete(account);
        accountRepository.flush();
    }

    private CloudAccount findAccount(
            UUID workspaceId,
            UUID accountId
    ) {
        if (workspaceId == null || accountId == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cloud account not found"
            );
        }

        return accountRepository.findByIdAndWorkspaceId(
                        accountId, workspaceId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cloud account not found"
                ));
    }

    private User requireActiveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        User user = userRepository.findByEmail(
                        email.trim().toLowerCase(Locale.ROOT)
                )
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

    private void requireWorkspace(UUID workspaceId) {
        if (workspaceId == null
                || !workspaceRepository.existsById(workspaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Workspace not found"
            );
        }
    }

    private WorkspaceMembership requireMembership(
            UUID workspaceId,
            UUID userId
    ) {
        if (workspaceId == null || userId == null) {
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

    private void requireWriter(UUID workspaceId, UUID userId) {
        WorkspaceMembership membership =
                requireMembership(workspaceId, userId);

        String role = membership.getRole();

        if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only workspace owners and admins can modify cloud accounts"
            );
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
    }

    private String normalize(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " is required"
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " exceeds the maximum length"
            );
        }

        return normalized;
    }
}
