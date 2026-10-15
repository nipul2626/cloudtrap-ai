package com.cloudtrap.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

public class LoginRateLimitFilter extends OncePerRequestFilter {
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final LoginRateLimitService limiter;
    private final SecurityErrorResponseWriter errors;

    public LoginRateLimitFilter(LoginRateLimitService limiter, SecurityErrorResponseWriter errors) {
        this.limiter = limiter; this.errors = errors;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest req) {
        return !"POST".equalsIgnoreCase(req.getMethod()) || !LOGIN_PATH.equals(req.getServletPath());
    }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        try {
            String key = ipKey(req.getRemoteAddr());
            if (!limiter.isAllowed(key, MAX_ATTEMPTS, WINDOW)) {
                errors.writeTooManyRequests(
        req,
        res,
        limiter.retryAfterSeconds(key, WINDOW)
);
                return;
            }
        } catch (RuntimeException ex) {
            // Fail closed rather than silently disabling brute-force protection.
            errors.writeServiceUnavailable(req, res);
            return;
        }
        chain.doFilter(req, res);
    }
    private String ipKey(String address) {
        String input = address == null || address.isBlank() ? "unknown" : address.trim();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
