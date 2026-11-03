package com.studywithus.identity.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserProfileUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GetUserProfileUseCase getUserProfileUseCase;

    private UpdateUserProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateUserProfileUseCase(userRepository, getUserProfileUseCase);
    }

    @Test
    void shouldUpdateUserProfileFieldsSuccessfully() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .username("testuser")
                .name("Old Name")
                .bio("Old Bio")
                .institution("Old Uni")
                .course("Old Course")
                .studyGoal("Old Goal")
                .status(UserStatus.ACTIVE)
                .lifetimePoints(50)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        UserProfileResult expectedResult = UserProfileResult.from(
                existingUser,
                50,
                2,
                5,
                3,
                7,
                Collections.emptyList()
        );
        when(getUserProfileUseCase.execute(userId)).thenReturn(expectedResult);

        UpdateUserProfileCommand command = new UpdateUserProfileCommand(
                userId,
                "New Name",
                "New Bio",
                "New Uni",
                "New Course",
                "New Goal",
                "https://avatar.url/img.png",
                LocalDate.of(1999, 5, 20)
        );

        UserProfileResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(existingUser.getName()).isEqualTo("New Name");
        assertThat(existingUser.getBio()).isEqualTo("New Bio");
        assertThat(existingUser.getInstitution()).isEqualTo("New Uni");
        assertThat(existingUser.getCourse()).isEqualTo("New Course");
        assertThat(existingUser.getStudyGoal()).isEqualTo("New Goal");
        assertThat(existingUser.getAvatarUrl()).isEqualTo("https://avatar.url/img.png");
        assertThat(existingUser.getBirthDate()).isEqualTo(LocalDate.of(1999, 5, 20));

        verify(userRepository).save(existingUser);
        verify(getUserProfileUseCase).execute(userId);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UpdateUserProfileCommand command = new UpdateUserProfileCommand(
                userId,
                "Name",
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("User not found");
    }
}
