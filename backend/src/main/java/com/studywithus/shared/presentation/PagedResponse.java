package com.studywithus.shared.presentation;

import com.studywithus.shared.domain.PagedResult;

import java.util.List;
import java.util.function.Function;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <E, D> PagedResponse<D> from(PagedResult<E> pagedResult, Function<E, D> mapper) {
        List<D> dtos = pagedResult.content().stream().map(mapper).toList();
        return new PagedResponse<>(
                dtos,
                pagedResult.page(),
                pagedResult.size(),
                pagedResult.totalElements(),
                pagedResult.totalPages(),
                pagedResult.hasNext()
        );
    }
}
