package com.studywithus.social.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public final class FeedCursorHelper {

    private FeedCursorHelper() {}

    public static Instant decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor.trim());
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            return Instant.parse(decodedString);
        } catch (IllegalArgumentException | DateTimeParseException ignored) {
            try {
                return Instant.parse(cursor.trim());
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    public static String encode(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(instant.toString().getBytes(StandardCharsets.UTF_8));
    }
}
