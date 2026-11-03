package com.studywithus.identity.domain;

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
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    UserCredential saveCredential(UserCredential credential);
    Optional<UserCredential> findCredentialByUserId(UUID userId);
}
