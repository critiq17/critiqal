package org.acme.api.dtos;

import java.util.List;

/*
    PageResponse - pagination response DTO
 */

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long total,
    boolean hasNext
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long total) {
        return new PageResponse<>(content, page, size, total, (long) (page + 1) * size < total);
    }
    public <R> PageResponse<R> map(java.util.function.Function<T, R> mapper) {
        return new PageResponse<>(
                content.stream().map(mapper).toList(),
                page, size, total, hasNext
        );
    }
}
