package com.studywithus.ranking.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.ranking.domain.RankingEntry;
import com.studywithus.ranking.domain.RankingEntryRepository;
import com.studywithus.ranking.domain.RankingPeriod;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetCommunityRankingUseCase {

    private final CommunityRepository communityRepository;
    private final RankingPeriodManager periodManager;
    private final RankingEntryRepository rankingEntryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResult<RankingEntryResult> execute(UUID communityId, int page, int size) {
        Community community = communityRepository.findById(communityId)
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        RankingPeriod period = periodManager.getOrCreateActivePeriod(communityId, community.getTimezone());

        PagedResult<RankingEntry> pagedEntries = rankingEntryRepository.findByPeriodIdOrderByPositionAsc(
                period.getId(),
                page,
                size
        );

        Set<UUID> userIds = pagedEntries.content().stream()
                .map(RankingEntry::getUserId)
                .collect(Collectors.toSet());

        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<RankingEntryResult> results = new ArrayList<>();
        for (RankingEntry entry : pagedEntries.content()) {
            User user = userMap.get(entry.getUserId());
            results.add(RankingEntryResult.of(
                    entry,
                    user != null ? user.getUsername() : "usuario",
                    user != null ? user.getName() : "Usuário",
                    user != null ? user.getAvatarUrl() : null
            ));
        }

        return new PagedResult<>(
                results,
                pagedEntries.page(),
                pagedEntries.size(),
                pagedEntries.totalElements(),
                pagedEntries.totalPages(),
                pagedEntries.hasNext()
        );
    }
}
