package com.studywithus.admin.application;

import com.studywithus.admin.domain.SystemLog;
import com.studywithus.admin.domain.SystemLogRepository;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuerySystemLogsUseCase {

    private final SystemLogRepository systemLogRepository;

    public record CursorPage(List<SystemLog> items, String nextCursor, boolean hasNext) {}

    @Transactional(readOnly = true)
    public CursorPage queryLogs(
            String level,
            String module,
            String traceId,
            UUID userId,
            Integer statusCode,
            Instant from,
            Instant to,
            String cursor,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Instant cursorTimestamp = null;
        UUID cursorId = null;

        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(cursor.trim()), StandardCharsets.UTF_8);
                String[] parts = decoded.split("\\|");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Malformed cursor");
                }
                cursorTimestamp = Instant.parse(parts[0]);
                cursorId = UUID.fromString(parts[1]);
            } catch (Exception e) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "Invalid pagination cursor");
            }
        }

        List<SystemLog> fetched = systemLogRepository.findByFilter(
                level, module, traceId, userId, statusCode, from, to, cursorTimestamp, cursorId, safeLimit + 1
        );

        boolean hasNext = fetched.size() > safeLimit;
        List<SystemLog> items = hasNext ? fetched.subList(0, safeLimit) : fetched;
        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            SystemLog last = items.get(items.size() - 1);
            String raw = last.getTimestamp().toString() + "|" + last.getId().toString();
            nextCursor = Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        return new CursorPage(items, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public List<SystemLog> getTraceLogs(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "TRACE_ID_REQUIRED", "traceId is required");
        }
        return systemLogRepository.findByTraceIdOrderByTimestampAsc(traceId.trim());
    }
}
