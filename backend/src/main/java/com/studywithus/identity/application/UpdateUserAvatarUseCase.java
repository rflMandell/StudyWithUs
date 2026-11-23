package com.studywithus.identity.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.media.application.MediaAssetResult;
import com.studywithus.media.application.ValidateMediaOwnershipUseCase;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateUserAvatarUseCase {

    private final UserRepository userRepository;
    private final ValidateMediaOwnershipUseCase validateMediaOwnershipUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;

    @Transactional
    public UserProfileResult execute(UUID userId, UUID mediaId) {
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        MediaAssetResult media = validateMediaOwnershipUseCase.validateAndResolveByMediaId(
                mediaId, userId, MediaPurpose.AVATAR
        );

        user.setAvatarUrl(media.url());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return getUserProfileUseCase.execute(userId);
    }
}
