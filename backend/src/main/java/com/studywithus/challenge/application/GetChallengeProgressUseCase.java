package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeParticipant;
import com.studywithus.challenge.domain.ChallengeRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetChallengeProgressUseCase implements UseCase<GetChallengeProgressQuery, ChallengeParticipantResult> {

    private final ChallengeRepository challengeRepository;

    @Override
    @Transactional(readOnly = true)
    public ChallengeParticipantResult execute(GetChallengeProgressQuery query) {
        Challenge challenge = challengeRepository.findById(query.challengeId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHALLENGE_NOT_FOUND", "Desafio não encontrado"));

        ChallengeParticipant participant = challengeRepository.findParticipant(query.challengeId(), query.userId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "PARTICIPANT_NOT_FOUND", "Usuário não está inscrito neste desafio"));

        return ChallengeParticipantResult.fromDomain(participant, challenge.getTargetValue());
    }
}
