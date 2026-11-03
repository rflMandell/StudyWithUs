package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinMedia;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.social.domain.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetUserFeedUseCase {

    private final CommunityMemberRepository communityMemberRepository;
    private final FollowRepository followRepository;
    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;

    @Transactional(readOnly = true)
    public FeedCursorResult execute(GetUserFeedQuery query) {
        int limit = query.limit() > 0 ? Math.min(query.limit(), 50) : 20;
        Instant cursorInstant = FeedCursorHelper.decode(query.cursor());

        List<UUID> activeCommunityIds = communityMemberRepository.findActiveCommunityIdsByUserId(query.userId());
        List<UUID> followingIds = followRepository.findFollowingIdsByFollowerId(query.userId());

        if (activeCommunityIds.isEmpty() && followingIds.isEmpty()) {
            return new FeedCursorResult(Collections.emptyList(), null, false);
        }

        List<Checkin> checkins = checkinRepository.findFeedCheckins(
                activeCommunityIds,
                followingIds,
                cursorInstant,
                limit + 1
        );

        boolean hasMore = checkins.size() > limit;
        List<Checkin> itemsToReturn = hasMore ? checkins.subList(0, limit) : checkins;

        String nextCursor = null;
        if (!itemsToReturn.isEmpty()) {
            Instant lastItemCreatedAt = itemsToReturn.get(itemsToReturn.size() - 1).getCreatedAt();
            nextCursor = FeedCursorHelper.encode(lastItemCreatedAt);
        }

        Set<UUID> userIds = itemsToReturn.stream()
                .map(Checkin::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> communityIds = itemsToReturn.stream()
                .map(Checkin::getCommunityId)
                .collect(Collectors.toSet());

        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<UUID, Community> communityMap = communityRepository.findAllById(communityIds).stream()
                .collect(Collectors.toMap(Community::getId, Function.identity()));

        List<FeedItemResult> results = new ArrayList<>();
        for (Checkin c : itemsToReturn) {
            User author = userMap.get(c.getUserId());
            Community community = communityMap.get(c.getCommunityId());

            List<String> mediaKeys = c.getMedia() != null
                    ? c.getMedia().stream().map(CheckinMedia::getStorageKey).toList()
                    : Collections.emptyList();

            results.add(new FeedItemResult(
                    c.getId(),
                    c.getUserId(),
                    author != null ? author.getUsername() : "usuario",
                    author != null ? author.getName() : "Usuário",
                    author != null ? author.getAvatarUrl() : null,
                    c.getCommunityId(),
                    community != null ? community.getName() : "Comunidade",
                    c.getCaption(),
                    mediaKeys,
                    c.getCreatedAt()
            ));
        }

        return new FeedCursorResult(results, nextCursor, hasMore);
    }
}
