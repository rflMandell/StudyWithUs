package com.studywithus.admin.application;

import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.RefreshTokenRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAccountManagementUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AdminAuditService adminAuditService;

    @Transactional
    public User suspendUser(UUID actorId, UUID targetUserId, String reason) {
        String cleanReason = requireReason(reason);
        if (actorId.equals(targetUserId)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "CANNOT_SUSPEND_SELF", "Administrators cannot suspend their own account");
        }

        User actor = requireUser(actorId);
        User target = requireUser(targetUserId);

        if (target.getEffectiveGlobalRole() == GlobalRole.SUPER_ADMIN
                && actor.getEffectiveGlobalRole() != GlobalRole.SUPER_ADMIN) {
            throw new DomainException(HttpStatus.FORBIDDEN, "CANNOT_MODIFY_SUPER_ADMIN", "Only SUPER_ADMIN can suspend a SUPER_ADMIN account");
        }

        UserStatus previousStatus = target.getStatus();
        Map<String, Object> beforeState = Map.of(
                "status", previousStatus.name(),
                "globalRole", target.getEffectiveGlobalRole().name()
        );

        target.setStatus(UserStatus.SUSPENDED);
        target.setUpdatedAt(Instant.now());
        User saved = userRepository.save(target);

        refreshTokenRepository.revokeAllByUserId(targetUserId);

        Map<String, Object> afterState = Map.of(
                "status", saved.getStatus().name(),
                "globalRole", saved.getEffectiveGlobalRole().name(),
                "sessionsRevoked", true
        );

        adminAuditService.record(
                actorId,
                "USER_SUSPENDED",
                "USER",
                targetUserId,
                cleanReason,
                beforeState,
                afterState
        );
        return saved;
    }

    @Transactional
    public User reactivateUser(UUID actorId, UUID targetUserId, String reason) {
        String cleanReason = requireReason(reason);
        User target = requireUser(targetUserId);

        UserStatus previousStatus = target.getStatus();
        Map<String, Object> beforeState = Map.of(
                "status", previousStatus.name(),
                "globalRole", target.getEffectiveGlobalRole().name()
        );

        target.setStatus(UserStatus.ACTIVE);
        target.setDeletedAt(null);
        target.setUpdatedAt(Instant.now());
        User saved = userRepository.save(target);

        Map<String, Object> afterState = Map.of(
                "status", saved.getStatus().name(),
                "globalRole", saved.getEffectiveGlobalRole().name()
        );

        adminAuditService.record(
                actorId,
                "USER_REACTIVATED",
                "USER",
                targetUserId,
                cleanReason,
                beforeState,
                afterState
        );
        return saved;
    }

    @Transactional
    public void revokeUserSessions(UUID actorId, UUID targetUserId, String reason) {
        String cleanReason = requireReason(reason);
        User target = requireUser(targetUserId);

        refreshTokenRepository.revokeAllByUserId(targetUserId);

        adminAuditService.record(
                actorId,
                "USER_SESSIONS_REVOKED",
                "USER",
                targetUserId,
                cleanReason,
                Map.of("username", target.getUsername(), "activeSessions", "UNKNOWN"),
                Map.of("username", target.getUsername(), "sessionsRevoked", true)
        );
    }

    @Transactional
    public User changeGlobalRole(UUID actorId, UUID targetUserId, GlobalRole newRole, String reason) {
        String cleanReason = requireReason(reason);
        if (newRole == null) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "GLOBAL_ROLE_REQUIRED", "Target globalRole is required");
        }
        if (actorId.equals(targetUserId)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "CANNOT_CHANGE_OWN_ROLE", "Administrators cannot change their own global role");
        }

        User actor = requireUser(actorId);
        if (actor.getEffectiveGlobalRole() != GlobalRole.SUPER_ADMIN) {
            throw new DomainException(HttpStatus.FORBIDDEN, "SUPER_ADMIN_REQUIRED", "Only SUPER_ADMIN can change global roles");
        }

        User target = requireUser(targetUserId);
        GlobalRole previousRole = target.getEffectiveGlobalRole();
        Map<String, Object> beforeState = Map.of(
                "globalRole", previousRole.name(),
                "status", target.getStatus().name()
        );

        target.setGlobalRole(newRole);
        target.setUpdatedAt(Instant.now());
        User saved = userRepository.save(target);

        refreshTokenRepository.revokeAllByUserId(targetUserId);

        Map<String, Object> afterState = Map.of(
                "globalRole", saved.getEffectiveGlobalRole().name(),
                "status", saved.getStatus().name(),
                "sessionsRevoked", true
        );

        adminAuditService.record(
                actorId,
                "USER_GLOBAL_ROLE_CHANGED",
                "USER",
                targetUserId,
                cleanReason,
                beforeState,
                afterState
        );
        return saved;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "REASON_REQUIRED", "Administrative action reason is required");
        }
        return reason.trim();
    }
}
