package com.studywithus.community.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.Optional;
import java.util.UUID;

public interface CommunityRepository {
    Community save(Community community);
    Optional<Community> findById(UUID id);
    boolean existsByNameIgnoreCase(String name);
    PagedResult<Community> findPublicCommunities(String search, int page, int size);
    java.util.List<Community> findAllById(Iterable<UUID> ids);
    long countByOwnerId(UUID ownerId);
}
