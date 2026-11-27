package com.ergoproxy.platform.mapping;

import com.ergoproxy.platform.model.dto.host.HostDTO;
import com.ergoproxy.platform.model.dto.host.reanime.ReanimeHostDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HostMapper {

    @Mapping(source = "serverName", target = "server")
    @Mapping(source = "dataLink", target = "embed")
    @Mapping(source = "dataType", target = "type")
    HostDTO reanimeHostToHost(ReanimeHostDTO source);

    List<HostDTO> reanimeHostToHostList(List<ReanimeHostDTO> source);
}
