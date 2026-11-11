package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeCreatedEvent;
import com.studywithus.challenge.domain.ChallengeRepository;
import com.studywithus.challenge.domain.ChallengeReward;
import com.studywithus.challenge.domain.ChallengeStatus;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateChallengeUseCase implements UseCase<CreateChallengeCommand, ChallengeResult> {

    private final ChallengeRepository challengeRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ChallengeResult execute(CreateChallengeCommand command) {
        userRepository.findById(command.creatorId())
                .filter(u -> u.isActive())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário criador não encontrado"));

        if (command.title() == null || command.title().isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_TITLE", "O título do desafio é obrigatório");
        }

        if (command.targetValue() <= 0) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_TARGET_VALUE", "A meta do desafio deve ser maior que zero");
        }

        Instant startAt = command.startAt() != null ? command.startAt() : Instant.now();
        Instant endAt = command.endAt() != null ? command.endAt() : startAt.plus(30, ChronoUnit.DAYS);

        if (endAt.isBefore(startAt)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "A data final deve ser posterior à data inicial");
        }

        if (command.communityId() != null) {
            communityRepository.findById(command.communityId())
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Comunidade não encontrada"));

            CommunityMember member = communityMemberRepository.findByCommunityIdAndUserId(command.communityId(), command.creatorId())
                    .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Usuário não é membro da comunidade"));

            if (!member.isAdmin()) {
                throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Apenas administradores ou proprietários da comunidade podem criar desafios");
            }
        }

        UUID challengeId = UUID.randomUUID();
        List<ChallengeReward> rewards = new ArrayList<>();
        if (command.rewards() != null) {
            for (CreateRewardCommand rc : command.rewards()) {
                rewards.add(ChallengeReward.builder()
                        .id(UUID.randomUUID())
                        .challengeId(challengeId)
                        .type(rc.type())
                        .value(rc.value())
                        .badgeId(rc.badgeId())
                        .build());
            }
        }

        Challenge challenge = Challenge.builder()
                .id(challengeId)
                .creatorId(command.creatorId())
                .communityId(command.communityId())
                .title(command.title().trim())
                .description(command.description() != null ? command.description().trim() : null)
                .scope(command.scope())
                .metric(command.metric())
                .targetValue(command.targetValue())
                .startAt(startAt)
                .endAt(endAt)
                .status(ChallengeStatus.ACTIVE)
                .rewards(rewards)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Challenge saved = challengeRepository.save(challenge);

        eventPublisher.publishEvent(new ChallengeCreatedEvent(
                saved.getId(),
                saved.getCreatorId(),
                saved.getCommunityId(),
                saved.getTitle()
        ));

        return ChallengeResult.fromDomain(saved);
    }
}
