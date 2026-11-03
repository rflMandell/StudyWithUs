package com.studywithus.identity.presentation;

import com.studywithus.identity.application.GetUserProfileUseCase;
import com.studywithus.identity.application.UpdateUserProfileUseCase;
import com.studywithus.identity.application.UserProfileResult;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;

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

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID userId) {
        UserProfileResult result = getUserProfileUseCase.execute(userId);
        return ResponseEntity.ok(UserProfileResponse.from(result));
    }
}
