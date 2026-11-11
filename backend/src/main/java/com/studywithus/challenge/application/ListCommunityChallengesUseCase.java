package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.ChallengeRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListCommunityChallengesUseCase implements UseCase<UUID, List<ChallengeResult>> {

    private final ChallengeRepository challengeRepository;
    private final CommunityRepository communityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResult> execute(UUID communityId) {
        communityRepository.findById(communityId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Comunidade não encontrada"));

        return challengeRepository.findByCommunityId(communityId)
                .stream()
                .map(ChallengeResult::fromDomain)
                .toList();
    }
}
