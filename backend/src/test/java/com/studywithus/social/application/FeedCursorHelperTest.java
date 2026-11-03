package com.studywithus.social.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldSupportRawIso8601String() {
        Instant expected = Instant.parse("2026-11-03T12:00:00Z");

        Instant decoded = FeedCursorHelper.decode("2026-11-03T12:00:00Z");
        assertThat(decoded).isEqualTo(expected);
    }

    @Test
    void shouldHandleNullAndBlankSafely() {
        assertThat(FeedCursorHelper.encode(null)).isNull();
        assertThat(FeedCursorHelper.decode(null)).isNull();
        assertThat(FeedCursorHelper.decode("   ")).isNull();
        assertThat(FeedCursorHelper.decode("invalid-cursor-string")).isNull();
    }
}
