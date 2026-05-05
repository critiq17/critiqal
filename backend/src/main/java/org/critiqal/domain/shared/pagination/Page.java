package org.critiqal.domain.shared.pagination;

import java.util.List;

public record Page<T>(
        List<T> content,
        int page,
        int size,
        long total,
        boolean hasNext
) {
    public static <T> Page<T> of(List<T> content, int page, int size, long total) {
        return new Page<>(content, page, size, total, (long) (page + 1) * size < total);
    }

    public <R> Page<R> map(java.util.function.Function<T, R> mapper) {
        return new Page<>(content.stream().map(mapper).toList(), page, size, total, hasNext);
    }
}
