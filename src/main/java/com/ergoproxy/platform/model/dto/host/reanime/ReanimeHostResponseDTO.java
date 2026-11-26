package com.ergoproxy.platform.model.dto.host.reanime;

import lombok.Data;

import java.util.List;

@Data
public class ReanimeHostResponseDTO {
    private Boolean success;
    private List<ReanimeHostDTO> servers;
}
