package com.studywithus.shared.infrastructure.logging;

import java.util.regex.Pattern;

/**
 * Masks sensitive credentials, tokens, secrets and PII before log emission or persistence.
 */
public final class SensitiveDataMasker {

    private static final Pattern BEARER_PATTERN =
            Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9\\-._~+/]+=*");
    private static final Pattern JWT_PATTERN =
            Pattern.compile("eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]*");
    private static final Pattern JSON_SECRET_PATTERN =
            Pattern.compile("(?i)(\"(?:password|passwordHash|secret|totpSecret|refreshToken|accessToken|token|apiKey|authorization)\"\\s*:\\s*\")([^\"]*)(\")");
    private static final Pattern KV_SECRET_PATTERN =
            Pattern.compile("(?i)\\b(password|passwordHash|secret|totpSecret|refreshToken|accessToken|token|apiKey)=([^\\s,&;]+)");

    private SensitiveDataMasker() {
    }

    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = BEARER_PATTERN.matcher(input).replaceAll("$1***MASKED***");
        result = JWT_PATTERN.matcher(result).replaceAll("***JWT_MASKED***");
        result = JSON_SECRET_PATTERN.matcher(result).replaceAll("$1***MASKED***$3");
        result = KV_SECRET_PATTERN.matcher(result).replaceAll("$1=***MASKED***");
        return result;
    }
}
