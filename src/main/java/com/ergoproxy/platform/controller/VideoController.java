package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.service.video.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.VIDEO)
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping(Const.Path.SEARCH)
    public List<?> search(@RequestBody SearchRequestDTO request) throws Exception {
        var response = videoService.search(request);
        return response;
    }
}
