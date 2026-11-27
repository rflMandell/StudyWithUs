package com.studywithus.identity.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByUsername(String username);
    Optional<UserJpaEntity> findByEmail(String email);

    @Query("SELECT u FROM UserJpaEntity u WHERE (u.username = :identifier OR u.email = :identifier) AND u.deletedAt IS NULL")
    Optional<UserJpaEntity> findByUsernameOrEmail(@Param("identifier") String identifier);

    @Query("""
            SELECT u FROM UserJpaEntity u
            WHERE u.status = com.studywithus.identity.domain.UserStatus.ACTIVE
              AND u.deletedAt IS NULL
              AND (LOWER(u.name) LIKE LOWER(CONCAT(:prefix, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT(:prefix, '%')))
            ORDER BY u.name ASC, u.username ASC
            """)
    List<UserJpaEntity> searchActiveByNameOrUsernamePrefix(@Param("prefix") String prefix, Pageable pageable);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
