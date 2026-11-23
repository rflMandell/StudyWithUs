package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.UserCosmeticRepository;
import com.studywithus.gamification.domain.UserLevel;
import com.studywithus.gamification.domain.UserLevelRepository;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserLevelUseCase {

    private final UserRepository userRepository;
    private final UserLevelRepository userLevelRepository;
    private final UserCosmeticRepository userCosmeticRepository;

    @Transactional(readOnly = true)
    public UserLevelResult execute(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        UserLevel userLevel = userLevelRepository.findByUserId(userId)
                .orElseGet(() -> UserLevel.initial(userId));

        return UserLevelResult.from(userLevel, userCosmeticRepository.findByUserId(userId));
    }
}
