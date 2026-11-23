package com.ergoproxy.platform.model.dto.pagination;

import lombok.Data;

@Data
public class PaginationDTO {
    private Integer page = 0;
    private Integer size = 20;
}
