package com.studywithus.media.application;

import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaAssetRepository;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestUploadUrlUseCase implements UseCase<RequestUploadUrlCommand, UploadUrlResult> {

    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectStoragePort objectStoragePort;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Value("${app.media.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes = 5242880L;

    @Value("${app.media.presign-expiration-seconds:300}")
    private long presignExpirationSeconds = 300L;

    @Override
    @Transactional
    public UploadUrlResult execute(RequestUploadUrlCommand command) {
        String ext = MediaValidationService.extractAndValidateExtension(command.filename());
        MediaValidationService.validateDeclaredContentType(command.contentType(), ext);
        MediaValidationService.validateSize(command.sizeBytes(), maxFileSizeBytes);

        if (command.purpose() == MediaPurpose.COMMUNITY_COVER && command.targetId() != null) {
            communityRepository.findById(command.targetId())
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

            CommunityMember member = communityMemberRepository.findByCommunityIdAndUserId(command.targetId(), command.userId())
                    .filter(CommunityMember::isActive)
                    .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only community admins or owners can upload a community cover"));

            if (!member.isAdmin()) {
                throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only community admins or owners can upload a community cover");
            }
        }

        String normalizedContentType = command.contentType().toLowerCase(Locale.ROOT);
        String storageKey = String.format("uploads/%s/%s/%s.%s",
                command.purpose().name().toLowerCase(Locale.ROOT),
                command.userId(),
                UUID.randomUUID(),
                ext);

        MediaAsset asset = MediaAsset.createPending(
                command.userId(),
                command.purpose(),
                storageKey,
                command.filename(),
                normalizedContentType,
                command.sizeBytes(),
                command.targetId()
        );

        MediaAsset saved = mediaAssetRepository.save(asset);

        Timer.Sample presignSample = meterRegistry != null ? Timer.start() : null;
        ObjectStoragePort.PresignedUploadResult presigned = objectStoragePort.generatePresignedUploadUrl(
                storageKey,
                normalizedContentType,
                command.sizeBytes(),
                Duration.ofSeconds(presignExpirationSeconds)
        );
        if (presignSample != null && meterRegistry != null) {
            presignSample.stop(Timer.builder("studywithus_media_presign_duration_seconds")
                    .description("Time spent generating presigned upload URLs")
                    .register(meterRegistry));
        }

        return new UploadUrlResult(
                saved.getId(),
                saved.getStorageKey(),
                presigned.presignedUrl(),
                presigned.expiresAt(),
                presigned.requiredHeaders()
        );
    }
}
