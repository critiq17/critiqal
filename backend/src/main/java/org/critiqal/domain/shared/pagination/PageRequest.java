package org.critiqal.domain.shared.pagination;

import jakarta.ws.rs.QueryParam;

public class PageRequest {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    @QueryParam("page")
    private int page = 0;

    @QueryParam("size")
    private int size = DEFAULT_SIZE;

    public int page() {
        return Math.max(page, 0);
    }

    public int size() {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public int offset() {
        return page() * size();
    }
}
