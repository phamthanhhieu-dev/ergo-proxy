package com.ergoproxy.platform.mapping;

import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.VideoResponseDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoMapper {
    VideoResponseDTO reanimeToVideo(ReanimeDTO source);

    List<VideoResponseDTO> reanimeToVideoList(List<ReanimeDTO> source);

    @Mapping(source = "title", target = "q")
    @Mapping(source = "size", target = "limit")
    @Mapping(target = "offset", expression = "java(source.getPage() * source.getSize())")
    ReanimeSearchRequestDTO searchRequestToReanimeSearchRequest(SearchRequestDTO source);
}
