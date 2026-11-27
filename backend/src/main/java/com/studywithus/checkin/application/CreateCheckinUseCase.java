package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.checkin.domain.StudyActivityRecordedEvent;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.media.application.ValidateMediaOwnershipUseCase;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.infrastructure.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateCheckinUseCase implements UseCase<CreateCheckinCommand, CheckinResult> {

    private final CheckinRepository checkinRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final ValidateMediaOwnershipUseCase validateMediaOwnershipUseCase;
    private final IdempotencyService idempotencyService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int LOCK_STRIPES = 256;
    private final Object[] idempotencyLocks = createLocks();

    private static Object[] createLocks() {
        Object[] locks = new Object[LOCK_STRIPES];
        for (int i = 0; i < LOCK_STRIPES; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    @Override
    @Transactional
    public CheckinResult execute(CreateCheckinCommand command) {
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            String cacheKey = "checkin:" + command.userId() + ":" + command.idempotencyKey();
            Object lock = idempotencyLocks[Math.floorMod(cacheKey.hashCode(), LOCK_STRIPES)];
            synchronized (lock) {
                return executeInternal(command, cacheKey);
            }
        }
        return executeInternal(command, null);
    }

    private CheckinResult executeInternal(CreateCheckinCommand command, String cacheKey) {
        if (cacheKey != null) {
            Optional<UUID> cachedId = idempotencyService.get(cacheKey, UUID.class);
            if (cachedId.isPresent()) {
                Optional<Checkin> existing = checkinRepository.findById(cachedId.get());
                if (existing.isPresent()) {
                    return CheckinResult.fromDomain(existing.get());
                }
            }
        }

        if (command.communityId() != null) {
            Community community = communityRepository.findById(command.communityId())
                    .filter(Community::isActive)
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

            CommunityMember member = communityMemberRepository.findByCommunityIdAndUserId(command.communityId(), command.userId())
                    .filter(CommunityMember::isActive)
                    .orElseThrow(() -> new DomainException(
                            HttpStatus.FORBIDDEN,
                            "COMMUNITY_NOT_A_MEMBER",
                            "User must be an active member of the community to create a check-in"
                    ));
        }

        if (command.mediaKeys() != null && !command.mediaKeys().isEmpty()) {
            validateMediaOwnershipUseCase.validateStorageKeys(command.mediaKeys(), command.userId(), MediaPurpose.CHECKIN);
        }

        boolean countsAsCheckin = command.communityId() != null
                || command.countsAsCheckin() == null
                || command.countsAsCheckin();

        Checkin checkin = Checkin.create(
                command.userId(),
                command.communityId(),
                command.caption(),
                command.mediaKeys(),
                command.subject(),
                command.tags(),
                command.durationMinutes(),
                countsAsCheckin
        );

        Checkin saved = checkinRepository.save(checkin);

        if (cacheKey != null) {
            idempotencyService.set(cacheKey, saved.getId(), 300);
        }

        eventPublisher.publishEvent(new CheckinCreatedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getCommunityId(),
                saved.getCreatedAt(),
                saved.isCountsAsCheckin()
        ));
        if (saved.isCountsAsCheckin()) {
            eventPublisher.publishEvent(new StudyActivityRecordedEvent(
                    saved.getId(),
                    saved.getUserId(),
                    saved.getCommunityId(),
                    saved.getSubject(),
                    saved.getTags(),
                    saved.getDurationMinutes(),
                    saved.getCreatedAt()
            ));
        }

        return CheckinResult.fromDomain(saved);
    }
}
