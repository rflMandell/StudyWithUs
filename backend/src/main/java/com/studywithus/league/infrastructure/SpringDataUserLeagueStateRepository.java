package com.studywithus.league.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataUserLeagueStateRepository extends JpaRepository<UserLeagueStateJpaEntity, UUID> {
}
