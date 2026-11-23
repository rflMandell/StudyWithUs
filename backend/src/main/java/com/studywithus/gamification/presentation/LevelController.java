package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.EquipCosmeticUseCase;
import com.studywithus.gamification.application.GetUserLevelUseCase;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
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
public class LevelController {

    private final GetUserLevelUseCase getUserLevelUseCase;
    private final EquipCosmeticUseCase equipCosmeticUseCase;

    @GetMapping("/me/level")
    public ResponseEntity<UserLevelResponse> getMyLevel(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return ResponseEntity.ok(UserLevelResponse.from(getUserLevelUseCase.execute(principal.getId())));
    }

    @GetMapping("/{userId}/level")
    public ResponseEntity<UserLevelResponse> getUserLevel(@PathVariable UUID userId) {
        return ResponseEntity.ok(UserLevelResponse.from(getUserLevelUseCase.execute(userId)));
    }

    @PatchMapping("/me/cosmetics")
    public ResponseEntity<UserLevelResponse> equipCosmetics(
            @RequestBody EquipCosmeticRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return ResponseEntity.ok(UserLevelResponse.from(
                equipCosmeticUseCase.execute(principal.getId(), request.title(), request.avatarBorder())
        ));
    }
}
