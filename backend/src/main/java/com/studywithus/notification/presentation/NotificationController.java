package com.studywithus.notification.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.notification.application.GetNotificationPreferencesUseCase;
import com.studywithus.notification.application.GetUnreadNotificationsCountUseCase;
import com.studywithus.notification.application.ListNotificationsQuery;
import com.studywithus.notification.application.ListNotificationsUseCase;
import com.studywithus.notification.application.MarkAllNotificationsAsReadUseCase;
import com.studywithus.notification.application.MarkNotificationAsReadCommand;
import com.studywithus.notification.application.MarkNotificationAsReadUseCase;
import com.studywithus.notification.application.NotificationPreferencesResult;
import com.studywithus.notification.application.NotificationResult;
import com.studywithus.notification.application.UpdateNotificationPreferencesUseCase;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.presentation.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ListNotificationsUseCase listNotificationsUseCase;
    private final MarkNotificationAsReadUseCase markNotificationAsReadUseCase;
    private final MarkAllNotificationsAsReadUseCase markAllNotificationsAsReadUseCase;
    private final GetUnreadNotificationsCountUseCase getUnreadNotificationsCountUseCase;
    private final GetNotificationPreferencesUseCase getNotificationPreferencesUseCase;
    private final UpdateNotificationPreferencesUseCase updateNotificationPreferencesUseCase;

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> listNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        PagedResult<NotificationResult> result = listNotificationsUseCase.execute(
                new ListNotificationsQuery(principal.getId(), unreadOnly, page, size)
        );
        return ResponseEntity.ok(PagedResponse.from(result, NotificationResponse::from));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        NotificationResult result = markNotificationAsReadUseCase.execute(
                new MarkNotificationAsReadCommand(id, principal.getId())
        );
        return ResponseEntity.ok(NotificationResponse.from(result));
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        markAllNotificationsAsReadUseCase.execute(principal.getId());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        long count = getUnreadNotificationsCountUseCase.execute(principal.getId());
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        NotificationPreferencesResult result = getNotificationPreferencesUseCase.execute(principal.getId());
        return ResponseEntity.ok(NotificationPreferencesResponse.from(result));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferencesRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        NotificationPreferencesResult result = updateNotificationPreferencesUseCase.execute(
                request.toCommand(principal.getId())
        );
        return ResponseEntity.ok(NotificationPreferencesResponse.from(result));
    }
}
