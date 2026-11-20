package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.service.subtitle.SubtitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.SUBTITLE)
public class SubtitleController {

    private final SubtitleService subtitleService;

    @GetMapping(Const.Path.SEARCH)
    public List<?> search(@RequestParam String source, @RequestParam String query, @RequestParam String language,
                          @RequestParam int season, @RequestParam int episode, @RequestParam String imdb,
                          @RequestParam boolean movie, @RequestParam int page) throws Exception {
        var response = subtitleService.search(source, query, language, season, episode, imdb, movie, page);
        return response;
    }
}
