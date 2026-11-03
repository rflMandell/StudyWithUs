package com.studywithus.social.domain;

import java.util.List;
import java.util.UUID;

public interface FollowRepository {
    Follow save(Follow follow);
    List<UUID> findFollowingIdsByFollowerId(UUID followerId);
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
}
