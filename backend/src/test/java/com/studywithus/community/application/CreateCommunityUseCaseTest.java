package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCommunityUseCaseTest {

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @InjectMocks
    private CreateCommunityUseCase createCommunityUseCase;

    private CreateCommunityCommand command;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        command = new CreateCommunityCommand(
                "Calculo 1 - Turma A",
                "Grupo de estudos para passar em cálculo",
                "https://example.com/image.png",
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo",
                ownerId
        );
    }

    @Test
    @DisplayName("Should create community and assign owner role to creator")
    void createCommunitySuccessfully() {
        when(communityRepository.existsByNameIgnoreCase("Calculo 1 - Turma A")).thenReturn(false);

        UUID communityId = UUID.randomUUID();
        when(communityRepository.save(any(Community.class))).thenAnswer(inv -> {
            Community c = inv.getArgument(0);
            c.setId(communityId);
            return c;
        });

        CommunityResult result = createCommunityUseCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(communityId);
        assertThat(result.name()).isEqualTo("Calculo 1 - Turma A");
        assertThat(result.ownerId()).isEqualTo(ownerId);
        assertThat(result.currentUserRole()).isEqualTo(CommunityRole.OWNER);
        assertThat(result.membersCount()).isEqualTo(1);

        verify(communityMemberRepository).save(any(CommunityMember.class));
    }

    @Test
    @DisplayName("Should throw conflict exception when community name already exists")
    void throwWhenNameExists() {
        when(communityRepository.existsByNameIgnoreCase("Calculo 1 - Turma A")).thenReturn(true);

        assertThatThrownBy(() -> createCommunityUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(de.getCode()).isEqualTo("COMMUNITY_NAME_ALREADY_EXISTS");
                });
    }
}
