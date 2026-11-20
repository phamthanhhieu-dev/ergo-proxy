package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.service.video.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.VIDEO)
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping(Const.Path.SEARCH)
    public List<?> search(@RequestParam String source, @RequestParam String query) throws Exception {
        var response = videoService.search(source, query);
        return response;
    }
}
