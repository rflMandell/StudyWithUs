package com.studywithus.identity.application;

import com.studywithus.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchUsersUseCase {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserSearchResult> execute(String query, int limit) {
        if (query == null) {
            return List.of();
        }
        String normalized = query.trim();
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1).trim();
        }
        if (normalized.isBlank()) {
            return List.of();
        }
        int clampedLimit = Math.max(1, Math.min(limit, 20));
        return userRepository.searchActiveByNameOrUsernamePrefix(normalized, clampedLimit).stream()
                .map(UserSearchResult::from)
                .toList();
    }
}
