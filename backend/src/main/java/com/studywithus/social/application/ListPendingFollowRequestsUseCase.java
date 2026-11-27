package com.studywithus.social.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.social.domain.FollowRequest;
import com.studywithus.social.domain.FollowRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListPendingFollowRequestsUseCase {

    private final FollowRequestRepository followRequestRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PendingFollowRequestResult> execute(UUID currentUserId) {
        List<FollowRequest> pending = followRequestRepository.findPendingByTargetId(currentUserId);
        if (pending.isEmpty()) {
            return List.of();
        }

        List<UUID> requesterIds = pending.stream()
                .map(FollowRequest::getRequesterId)
                .distinct()
                .toList();

        Map<UUID, User> usersById = userRepository.findAllById(requesterIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return pending.stream()
                .filter(req -> usersById.containsKey(req.getRequesterId()))
                .map(req -> {
                    User requester = usersById.get(req.getRequesterId());
                    return new PendingFollowRequestResult(
                            req.getId(),
                            requester.getId(),
                            requester.getUsername(),
                            requester.getName(),
                            requester.getAvatarUrl(),
                            req.getCreatedAt()
                    );
                })
                .toList();
    }
}
