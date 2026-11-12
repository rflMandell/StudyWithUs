package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.CommunityVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SpringDataCommunityRepository extends JpaRepository<CommunityJpaEntity, UUID> {

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    long countByOwnerIdAndDeletedAtIsNull(UUID ownerId);

    @Query("""
        SELECT c FROM CommunityJpaEntity c
        WHERE c.visibility = :visibility
          AND c.deletedAt IS NULL
          AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY c.createdAt DESC
    """)
    Page<CommunityJpaEntity> findByVisibilityAndSearch(
            @Param("visibility") CommunityVisibility visibility,
            @Param("search") String search,
            Pageable pageable
    );
}
