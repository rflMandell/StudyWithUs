package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.PointTransaction;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListUserPointsHistoryUseCase {

    private final PointTransactionRepository pointTransactionRepository;

    @Transactional(readOnly = true)
    public PagedResult<PointTransactionResult> execute(UUID userId, int page, int size) {
        PagedResult<PointTransaction> paged = pointTransactionRepository.findByUserId(userId, page, size);

        List<PointTransactionResult> items = paged.content().stream()
                .map(PointTransactionResult::fromDomain)
                .toList();

        return new PagedResult<>(
                items,
                paged.page(),
                paged.size(),
                paged.totalElements(),
                paged.totalPages(),
                paged.hasNext()
        );
    }
}
