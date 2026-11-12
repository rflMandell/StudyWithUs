package com.studywithus.notification.application;

import com.studywithus.notification.domain.InAppNotification;
import com.studywithus.notification.domain.NotificationRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListNotificationsUseCase implements UseCase<ListNotificationsQuery, PagedResult<NotificationResult>> {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<NotificationResult> execute(ListNotificationsQuery query) {
        PagedResult<InAppNotification> paged = notificationRepository.findByUserId(
                query.userId(),
                query.unreadOnly(),
                query.page(),
                query.size()
        );

        return PagedResult.of(
                paged.content().stream().map(NotificationResult::fromDomain).toList(),
                paged.page(),
                paged.size(),
                paged.totalElements()
        );
    }
}
