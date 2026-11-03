package com.studywithus.identity.infrastructure;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserCredential;
import com.studywithus.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataUserCredentialRepository springDataUserCredentialRepository;

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        return springDataUserRepository.save(entity).toDomain();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return springDataUserRepository.findByUsernameOrEmail(usernameOrEmail).map(UserJpaEntity::toDomain);
    }

    @Override
    public List<User> findAllById(Iterable<UUID> ids) {
        return springDataUserRepository.findAllById(ids).stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    @Override
    public UserCredential saveCredential(UserCredential credential) {
        UserCredentialJpaEntity entity = UserCredentialJpaEntity.fromDomain(credential);
        return springDataUserCredentialRepository.save(entity).toDomain();
    }

    @Override
    public Optional<UserCredential> findCredentialByUserId(UUID userId) {
        return springDataUserCredentialRepository.findById(userId).map(UserCredentialJpaEntity::toDomain);
    }
}
