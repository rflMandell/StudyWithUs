package com.studywithus.identity.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SearchUsersUseCase useCase;

    @Test
    void returnsEmptyListForNullOrBlankOrAtOnlyQuery() {
        assertThat(useCase.execute(null, 10)).isEmpty();
        assertThat(useCase.execute("   ", 10)).isEmpty();
        assertThat(useCase.execute("@", 10)).isEmpty();
        assertThat(useCase.execute("  @  ", 10)).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void stripsLeadingAtAndTrimsAndClampsLimit() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("alice_dev")
                .name("Alice")
                .isPrivate(true)
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.searchActiveByNameOrUsernamePrefix("alice", 20)).thenReturn(List.of(user));
        when(userRepository.searchActiveByNameOrUsernamePrefix("bob", 1)).thenReturn(List.of());

        List<UserSearchResult> results = useCase.execute("  @alice  ", 100);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).username()).isEqualTo("alice_dev");
        assertThat(results.get(0).isPrivate()).isTrue();
        verify(userRepository).searchActiveByNameOrUsernamePrefix("alice", 20);

        useCase.execute("bob", 0);
        verify(userRepository).searchActiveByNameOrUsernamePrefix("bob", 1);
    }
}
