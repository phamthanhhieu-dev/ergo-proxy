package com.ergoproxy.platform.dto;

public class PaginationDTO {

    private Integer page = 0;
    private Integer size = 20;
    private Long total;
    private Integer totalPages;

    public PaginationDTO() {
    }

    public PaginationDTO(Integer page, Integer size, Long total, Integer totalPages) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = totalPages;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
}
