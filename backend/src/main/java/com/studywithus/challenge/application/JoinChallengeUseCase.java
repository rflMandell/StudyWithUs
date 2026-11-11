package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeParticipant;
import com.studywithus.challenge.domain.ChallengeParticipantJoinedEvent;
import com.studywithus.challenge.domain.ChallengeRepository;
import com.studywithus.challenge.domain.ChallengeStatus;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JoinChallengeUseCase implements UseCase<JoinChallengeCommand, ChallengeParticipantResult> {

    private final ChallengeRepository challengeRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ChallengeParticipantResult execute(JoinChallengeCommand command) {
        userRepository.findById(command.userId())
                .filter(u -> u.isActive())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário não encontrado"));

        Challenge challenge = challengeRepository.findById(command.challengeId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHALLENGE_NOT_FOUND", "Desafio não encontrado"));

        Instant now = Instant.now();
        if (challenge.getStatus() != ChallengeStatus.ACTIVE || (challenge.getEndAt() != null && now.isAfter(challenge.getEndAt()))) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "CHALLENGE_NOT_ACTIVE", "Este desafio não está ativo");
        }

        if (challenge.getCommunityId() != null) {
            boolean isMember = communityMemberRepository.isUserActiveMember(challenge.getCommunityId(), command.userId());
            if (!isMember) {
                throw new DomainException(
                        HttpStatus.FORBIDDEN,
                        "COMMUNITY_NOT_A_MEMBER",
                        "Apenas membros ativos da comunidade podem participar deste desafio"
                );
            }
        }

        Optional<ChallengeParticipant> existing = challengeRepository.findParticipant(command.challengeId(), command.userId());
        if (existing.isPresent()) {
            return ChallengeParticipantResult.fromDomain(existing.get(), challenge.getTargetValue());
        }

        ChallengeParticipant participant = ChallengeParticipant.join(command.challengeId(), command.userId());
        ChallengeParticipant saved = challengeRepository.saveParticipant(participant);

        eventPublisher.publishEvent(new ChallengeParticipantJoinedEvent(challenge.getId(), command.userId()));

        return ChallengeParticipantResult.fromDomain(saved, challenge.getTargetValue());
    }
}
