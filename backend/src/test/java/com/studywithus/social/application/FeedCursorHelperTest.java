package com.studywithus.social.application;

import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedCursorHelperTest {

    @Test
    void shouldEncodeAndDecodeInstantCorrectly() {
        Instant now = Instant.parse("2026-11-03T15:30:00Z");

        String encoded = FeedCursorHelper.encode(now);
        assertThat(encoded).isNotBlank();

        Instant decoded = FeedCursorHelper.decode(encoded);
        assertThat(decoded).isEqualTo(now);
    }

    @Test
    void shouldEncodeAndDecodeCompositeCursorCorrectly() {
        Instant now = Instant.parse("2026-11-03T15:30:00Z");
        UUID id = UUID.randomUUID();

        String encoded = FeedCursorHelper.encode(now, id);
        assertThat(encoded).isNotBlank();

        FeedCursorHelper.FeedCursor decoded = FeedCursorHelper.decodeCursor(encoded);
        assertThat(decoded).isNotNull();
        assertThat(decoded.createdAt()).isEqualTo(now);
        assertThat(decoded.id()).isEqualTo(id);
    }

    @Test
    void shouldSupportRawIso8601String() {
        Instant expected = Instant.parse("2026-11-03T12:00:00Z");

        Instant decoded = FeedCursorHelper.decode("2026-11-03T12:00:00Z");
        assertThat(decoded).isEqualTo(expected);
    }

    @Test
    void shouldHandleNullAndBlankSafelyAndThrowBadRequestOnMalformed() {
        assertThat(FeedCursorHelper.encode(null)).isNull();
        assertThat(FeedCursorHelper.decode(null)).isNull();
        assertThat(FeedCursorHelper.decode("   ")).isNull();
        assertThatThrownBy(() -> FeedCursorHelper.decode("invalid-cursor-string"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
