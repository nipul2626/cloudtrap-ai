
package com.cloudtrap.api.identity.controller;

import com.cloudtrap.api.common.dto.ApiErrorResponse;
import com.cloudtrap.api.common.filter.RequestIdFilter;
import com.cloudtrap.api.identity.dto.LoginRequest;
import com.cloudtrap.api.identity.dto.LoginResponse;
import com.cloudtrap.api.identity.service.RegistrationValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthenticationController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    /**
     * Returns the CSRF token required by clients for protected state-changing
     * requests, including login and registration when CSRF protection is enabled.
     */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName()
        );
    }

    /**
     * Authenticates a user and persists the security context in the HTTP session.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        final String email;

        try {
            email = RegistrationValidator.normalizeEmail(body.email());
        } catch (IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();

            return error(
                    request,
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Invalid email or password"
            );
        }

        final Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email,
                            body.password()
                    )
            );
        } catch (AuthenticationServiceException ex) {
            SecurityContextHolder.clearContext();

            return error(
                    request,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AUTHENTICATION_UNAVAILABLE",
                    "Authentication is temporarily unavailable"
            );
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();

            return error(
                    request,
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Invalid email or password"
            );
        }

        try {
            // Create the session before applying the configured session strategy.
            request.getSession(true);

            // Applies session fixation protection after successful authentication.
            sessionAuthenticationStrategy.onAuthentication(
                    authentication,
                    request,
                    response
            );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Explicitly save the context because requireExplicitSave is enabled.
            securityContextRepository.saveContext(
                    context,
                    request,
                    response
            );

            return ResponseEntity.ok()
                    .header("Cache-Control", "no-store")
                    .header("Pragma", "no-cache")
                    .body(new LoginResponse(true, email));

        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();

            return error(
                    request,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SESSION_CREATION_FAILED",
                    "Unable to establish a session"
            );
        }
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message
    ) {
        Object attribute = request.getAttribute(
                RequestIdFilter.REQUEST_ATTRIBUTE
        );

        String requestId =
                attribute instanceof String value && !value.isBlank()
                        ? value
                        : UUID.randomUUID().toString();

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                requestId,
                List.of()
        );

        return ResponseEntity.status(status)
                .header(RequestIdFilter.HEADER_NAME, requestId)
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(body);
    }

    public record CsrfResponse(
            String token,
            String headerName
    ) {
    }
}
