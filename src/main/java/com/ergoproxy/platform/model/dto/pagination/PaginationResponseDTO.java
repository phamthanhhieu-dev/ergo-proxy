package com.ergoproxy.platform.model.dto.pagination;

import lombok.Data;

@Data
public class PaginationResponseDTO extends PaginationDTO {
    private Long total;
    private Integer totalPages;
}
