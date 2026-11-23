package com.ergoproxy.platform.util;

public final class PageUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private PageUtils() {
    }

    public static int getPage(Integer page) {
        int response = page == null || page < 0 ? DEFAULT_PAGE : page;
        return response;
    }

    public static int getSize(Integer size) {
        int response = size == null || size <= 0 ? DEFAULT_SIZE : size;
        return response;
    }

    public static int getTotalPages(long total, int size) {
        int response = (int) Math.ceil((double) total / size);
        return response;
    }

    public static boolean isInPage(long index, int page, int size) {
        boolean response = index >= (long) page * size && index < (long) (page + 1) * size;
        return response;
    }

    public static <T extends PaginationResponseDTO> void setPagination(T response, long total, int page, int size) {
        response.setPage(page);
        response.setSize(size);
        response.setTotal(total);
        response.setTotalPages(getTotalPages(total, size));
    }
}
