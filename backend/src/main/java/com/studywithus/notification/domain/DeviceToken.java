package com.studywithus.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {
    private UUID id;
    private UUID userId;
    private String token;
    private String platform;
    @Builder.Default
    private boolean active = true;
    private Instant createdAt;
    private Instant lastSeenAt;

    public static DeviceToken create(UUID userId, String token, String platform) {
        Instant now = Instant.now();
        return DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token(token)
                .platform(platform != null ? platform.toUpperCase() : "WEB")
                .active(true)
                .createdAt(now)
                .lastSeenAt(now)
                .build();
    }

    public void deactivate() {
        this.active = false;
        this.lastSeenAt = Instant.now();
    }

    public void updateLastSeen() {
        this.active = true;
        this.lastSeenAt = Instant.now();
    }
}
