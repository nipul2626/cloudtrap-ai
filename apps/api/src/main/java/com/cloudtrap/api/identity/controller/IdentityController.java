
package com.cloudtrap.api.identity.controller;

import com.cloudtrap.api.identity.dto.RegisterRequest;
import com.cloudtrap.api.identity.dto.RegisterResponse;
import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * HTTP endpoints for user identity and authentication operations.
 */
@RestController
@RequestMapping(
        path = "/api/v1/auth",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = Objects.requireNonNull(
                identityService,
                "identityService must not be null"
        );
    }

    /**
     * Registers a new user account.
     *
     * @param request validated registration details
     * @return HTTP 201 with the public representation of the new user
     */
    @PostMapping(
            path = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User registeredUser = identityService.register(
                request.email(),
                request.password(),
                request.displayName()
        );

        RegisterResponse response = new RegisterResponse(
                registeredUser.getId(),
                registeredUser.getEmail(),
                registeredUser.getDisplayName(),
                registeredUser.getCreatedAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
