package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinMedia;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.gamification.domain.LevelCosmeticCatalog;
import com.studywithus.gamification.domain.UserLevel;
import com.studywithus.gamification.domain.UserLevelRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.social.domain.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FeedItemHydrator {

    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final CommentRepository commentRepository;
    private final UserLevelRepository userLevelRepository;

    @Autowired
    public FeedItemHydrator(
            UserRepository userRepository,
            CommunityRepository communityRepository,
            CommentRepository commentRepository,
            @Autowired(required = false) UserLevelRepository userLevelRepository
    ) {
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
        this.commentRepository = commentRepository;
        this.userLevelRepository = userLevelRepository;
    }

    public FeedItemHydrator(
            UserRepository userRepository,
            CommunityRepository communityRepository,
            CommentRepository commentRepository
    ) {
        this(userRepository, communityRepository, commentRepository, null);
    }

    public List<Checkin> hydrateInOrder(List<UUID> orderedIds, CheckinRepository checkinRepository) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<UUID, Checkin> byId = checkinRepository.findAllActiveByIds(orderedIds).stream()
                .collect(Collectors.toMap(Checkin::getId, Function.identity()));

        List<Checkin> ordered = new ArrayList<>(orderedIds.size());
        for (UUID id : orderedIds) {
            Checkin checkin = byId.get(id);
            if (checkin != null) {
                ordered.add(checkin);
            }
        }
        return ordered;
    }

    public FeedCursorResult toCursorResult(List<Checkin> checkins, int limit) {
        if (checkins == null || checkins.isEmpty()) {
            return new FeedCursorResult(Collections.emptyList(), null, false);
        }

        boolean hasMore = checkins.size() > limit;
        List<Checkin> itemsToReturn = hasMore ? checkins.subList(0, limit) : checkins;

        String nextCursor = null;
        if (hasMore && !itemsToReturn.isEmpty()) {
            Checkin lastItem = itemsToReturn.get(itemsToReturn.size() - 1);
            nextCursor = FeedCursorHelper.encode(lastItem.getCreatedAt(), lastItem.getId());
        }

        Set<UUID> userIds = itemsToReturn.stream()
                .map(Checkin::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> communityIds = itemsToReturn.stream()
                .map(Checkin::getCommunityId)
                .collect(Collectors.toSet());

        List<UUID> checkinIds = itemsToReturn.stream()
                .map(Checkin::getId)
                .toList();

        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<UUID, Community> communityMap = communityRepository.findAllById(communityIds).stream()
                .collect(Collectors.toMap(Community::getId, Function.identity()));

        Map<UUID, Long> commentCountsMap = commentRepository.countActiveByCheckinIds(checkinIds);

        Map<UUID, UserLevel> userLevelMap = userLevelRepository != null
                ? userLevelRepository.findAllByUserIds(userIds).stream()
                        .collect(Collectors.toMap(UserLevel::getUserId, Function.identity()))
                : Collections.emptyMap();

        String defaultTitle = LevelCosmeticCatalog.getDefaultTitleForLevel(1);
        String defaultBorder = LevelCosmeticCatalog.getDefaultBorderForLevel(1);

        List<FeedItemResult> results = new ArrayList<>(itemsToReturn.size());
        for (Checkin c : itemsToReturn) {
            User author = userMap.get(c.getUserId());
            Community community = communityMap.get(c.getCommunityId());
            UserLevel authorLevel = userLevelMap.get(c.getUserId());

            List<String> mediaKeys = c.getMedia() != null
                    ? c.getMedia().stream().map(CheckinMedia::getStorageKey).toList()
                    : Collections.emptyList();

            long commentsCount = commentCountsMap.getOrDefault(c.getId(), 0L);

            results.add(new FeedItemResult(
                    c.getId(),
                    c.getUserId(),
                    author != null ? author.getUsername() : "usuario",
                    author != null ? author.getName() : "Usuário",
                    author != null ? author.getAvatarUrl() : null,
                    authorLevel != null ? authorLevel.getCurrentLevel() : 1,
                    authorLevel != null ? authorLevel.getActiveTitle() : defaultTitle,
                    authorLevel != null ? authorLevel.getActiveAvatarBorder() : defaultBorder,
                    c.getCommunityId(),
                    community != null ? community.getName() : "Comunidade",
                    c.getCaption(),
                    mediaKeys,
                    commentsCount,
                    c.getCreatedAt()
            ));
        }

        return new FeedCursorResult(results, nextCursor, hasMore);
    }
}
