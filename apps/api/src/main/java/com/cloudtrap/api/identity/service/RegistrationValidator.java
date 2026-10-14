
package com.cloudtrap.api.identity.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RegistrationValidator {

    private static final int MAX_EMAIL_LENGTH = 320;
    private static final int MIN_PASSWORD_CODE_POINTS = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final int MAX_DISPLAY_NAME_LENGTH = 120;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
                    + "@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    private RegistrationValidator() {
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException(
                    "Email must not exceed 320 characters"
            );
        }

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email address is invalid");
        }

        return normalized;
    }

    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }

        int codePointCount = password.codePointCount(0, password.length());

        if (codePointCount < MIN_PASSWORD_CODE_POINTS) {
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters"
            );
        }

        int utf8Length = password.getBytes(StandardCharsets.UTF_8).length;

        if (utf8Length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException(
                    "Password must not exceed 72 UTF-8 bytes"
            );
        }
    }

    public static String validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Display name must not be blank"
            );
        }

        String normalized = displayName.trim();

        if (normalized.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Display name must not exceed 120 characters"
            );
        }

        return normalized;
    }
}
