package com.studywithus.social.application;

import com.studywithus.shared.domain.DomainException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

public final class FeedCursorHelper {

    public record FeedCursor(Instant createdAt, UUID id) {}

    private FeedCursorHelper() {}

    public static FeedCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String trimmed = cursor.trim();
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(trimmed);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            int separatorIndex = decodedString.indexOf('|');
            if (separatorIndex >= 0) {
                String timestampPart = decodedString.substring(0, separatorIndex);
                String idPart = decodedString.substring(separatorIndex + 1);
                return new FeedCursor(Instant.parse(timestampPart), UUID.fromString(idPart));
            }
            return new FeedCursor(Instant.parse(decodedString), null);
        } catch (IllegalArgumentException | DateTimeParseException ignored) {
            try {
                return new FeedCursor(Instant.parse(trimmed), null);
            } catch (DateTimeParseException e) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_FEED_CURSOR", "Cursor de paginação inválido");
            }
        }
    }

    public static Instant decode(String cursor) {
        FeedCursor decoded = decodeCursor(cursor);
        return decoded != null ? decoded.createdAt() : null;
    }

    public static String encode(Instant instant) {
        return encode(instant, null);
    }

    public static String encode(Instant instant, UUID id) {
        if (instant == null) {
            return null;
        }
        String raw = id != null ? instant.toString() + "|" + id : instant.toString();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
