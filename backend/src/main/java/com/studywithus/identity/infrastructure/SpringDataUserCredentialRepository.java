package com.studywithus.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataUserCredentialRepository extends JpaRepository<UserCredentialJpaEntity, UUID> {
}
