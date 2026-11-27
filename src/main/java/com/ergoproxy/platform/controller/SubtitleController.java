package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.constant.Language;
import com.ergoproxy.platform.service.subtitle.SubtitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.SUBTITLE)
public class SubtitleController {

    private final SubtitleService subtitleService;

    @GetMapping(Const.Path.SEARCH)
    public List<?> search(@RequestParam String source, @RequestParam String movieName,
                          @RequestParam(defaultValue = Language.VIETNAM) String language, @RequestParam(required = false) Integer seasonNumber,
                          @RequestParam(required = false) Integer episodeNumber, @RequestParam(required = false) Integer year,
                          @RequestParam(required = false) String type,
                          @RequestParam(defaultValue = Const.Common.DEFAULT_PAGE_INDEX) Integer page,
                          @RequestParam(defaultValue = Const.Common.DEFAULT_PAGE_SIZE) Integer size) throws Exception {
        var response = subtitleService.search(source, movieName, language, seasonNumber, episodeNumber, year, type, page, size);
        return response;
    }
}
