package com.ergoproxy.platform.dto;

import java.util.List;

public class PaginationResponseDTO<T> extends PaginationDTO {

    private List<T> items;

    public PaginationResponseDTO() {}

    public PaginationResponseDTO(List<T> items) {
        this.items = items;
    }

    public PaginationResponseDTO(Integer page, Integer size, Long total, Integer totalPages, List<T> items) {
        super(page, size, total, totalPages);
        this.items = items;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}
