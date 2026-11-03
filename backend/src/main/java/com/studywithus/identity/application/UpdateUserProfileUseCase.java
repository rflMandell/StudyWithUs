package com.studywithus.identity.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserProfileUseCase {

    private final UserRepository userRepository;
    private final GetUserProfileUseCase getUserProfileUseCase;

    @Transactional
    public UserProfileResult execute(UpdateUserProfileCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        user.updateProfile(
                command.name(),
                command.bio(),
                command.institution(),
                command.course(),
                command.studyGoal(),
                command.avatarUrl(),
                command.birthDate()
        );

        userRepository.save(user);

        return getUserProfileUseCase.execute(command.userId());
    }
}
