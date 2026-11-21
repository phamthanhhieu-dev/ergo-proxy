package com.ergoproxy.platform.mapping;

import com.ergoproxy.platform.model.dto.video.VideoDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoMapper {
    VideoDTO reanimeToVideo(ReanimeDTO source);

    List<VideoDTO> reanimeToVideoList(List<ReanimeDTO> source);
}
