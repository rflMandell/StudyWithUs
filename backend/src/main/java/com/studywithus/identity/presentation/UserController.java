package com.studywithus.identity.presentation;

import com.studywithus.identity.application.ChangePasswordUseCase;
import com.studywithus.identity.application.DeactivateAccountUseCase;
import com.studywithus.identity.application.DeleteAccountUseCase;
import com.studywithus.identity.application.GetUserProfileUseCase;
import com.studywithus.identity.application.SearchUsersUseCase;
import com.studywithus.identity.application.UpdateAccountPrivacyUseCase;
import com.studywithus.identity.application.UpdateUserAvatarUseCase;
import com.studywithus.identity.application.UpdateUserProfileUseCase;
import com.studywithus.identity.application.UserProfileResult;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.FollowRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final SearchUsersUseCase searchUsersUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final UpdateUserAvatarUseCase updateUserAvatarUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final UpdateAccountPrivacyUseCase updateAccountPrivacyUseCase;
    private final DeactivateAccountUseCase deactivateAccountUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final FollowRepository followRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal SecurityUserPrincipal principal) {
        UserProfileResult result = getUserProfileUseCase.execute(principal.getId());
        return ResponseEntity.ok(UserProfileResponse.from(result));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserProfileRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UserProfileResult result = updateUserProfileUseCase.execute(request.toCommand(principal.getId()));
        return ResponseEntity.ok(UserProfileResponse.from(result));
    }

    @PostMapping("/me/avatar/upload")
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            @Valid @RequestBody UpdateAvatarRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UserProfileResult result = updateUserAvatarUseCase.execute(principal.getId(), request.mediaId());
        return ResponseEntity.ok(UserProfileResponse.from(result));
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        changePasswordUseCase.execute(request.toCommand(principal.getId()));
    }

    @PatchMapping("/me/privacy")
    public ResponseEntity<UserProfileResponse> updatePrivacy(
            @Valid @RequestBody UpdatePrivacyRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UserProfileResult result = updateAccountPrivacyUseCase.execute(request.toCommand(principal.getId()));
        return ResponseEntity.ok(UserProfileResponse.from(result));
    }

    @PostMapping("/me/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateAccount(@AuthenticationPrincipal SecurityUserPrincipal principal) {
        deactivateAccountUseCase.execute(principal.getId());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal SecurityUserPrincipal principal) {
        deleteAccountUseCase.execute(principal.getId());
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchItemResponse>> searchUsers(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<UserSearchItemResponse> results = searchUsersUseCase.execute(query, limit).stream()
                .map(UserSearchItemResponse::from)
                .toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(
            @PathVariable UUID userId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UserProfileResult result = getUserProfileUseCase.execute(userId);
        boolean isSelf = principal != null && principal.getId().equals(userId);
        if (!isSelf && !result.status().equals(UserStatus.ACTIVE)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
        if (isSelf) {
            return ResponseEntity.ok(UserProfileResponse.from(result));
        }
        boolean isFollowing = principal != null && followRepository.existsByFollowerIdAndFollowingId(principal.getId(), userId);
        return ResponseEntity.ok(UserProfileResponse.fromPublic(result, isFollowing));
    }
}
