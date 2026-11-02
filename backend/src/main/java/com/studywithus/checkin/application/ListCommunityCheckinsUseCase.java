package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListCommunityCheckinsUseCase {

    private final CheckinRepository checkinRepository;

    @Transactional(readOnly = true)
    public PagedResult<CheckinResult> execute(UUID communityId, int page, int size) {
        PagedResult<Checkin> pagedResult = checkinRepository.findByCommunityIdActive(communityId, page, size);

        List<CheckinResult> content = pagedResult.content().stream()
                .map(CheckinResult::fromDomain)
                .toList();

        return new PagedResult<>(
                content,
                pagedResult.page(),
                pagedResult.size(),
                pagedResult.totalElements(),
                pagedResult.totalPages(),
                pagedResult.hasNext()
        );
    }
}
