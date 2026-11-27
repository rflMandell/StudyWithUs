package com.studywithus.identity.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
    List<User> findAllById(Iterable<UUID> ids);
    PagedResult<User> findAll(int page, int size);
    List<User> searchActiveByNameOrUsernamePrefix(String prefix, int limit);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    UserCredential saveCredential(UserCredential credential);
    Optional<UserCredential> findCredentialByUserId(UUID userId);
}
