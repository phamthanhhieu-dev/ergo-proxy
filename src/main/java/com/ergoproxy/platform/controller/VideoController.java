package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.service.video.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.VIDEO)
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping(Const.Path.SEARCH)
    public ResponseEntity<?> search(@RequestParam String source, @RequestParam(required = false) String query,
                                    @RequestParam(required = false) String genre, @RequestParam(required = false) String season,
                                    @RequestParam(required = false) String format, @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String country, @RequestParam(required = false) String audio,
                                    @RequestParam(required = false) String rating, @RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) String sort, @RequestParam(required = false) String tag,
                                    @RequestParam(required = false) String character, @RequestParam(required = false) String staff,
                                    @RequestParam(required = false) String studio,
                                    @RequestParam(defaultValue = Const.Common.DEFAULT_PAGE_INDEX) Integer page,
                                    @RequestParam(defaultValue = Const.Common.DEFAULT_PAGE_SIZE) Integer size) throws Exception {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setSource(source);
        request.setTitle(query);
        request.setGenre(genre);
        request.setSeason(season);
        request.setFormat(format);
        request.setStatus(status);
        request.setCountry(country);
        request.setAudio(audio);
        request.setRating(rating);
        request.setYear(year);
        request.setSort(sort);
        request.setTag(tag);
        request.setCharacter(character);
        request.setStaff(staff);
        request.setStudio(studio);
        request.setPage(page);
        request.setSize(size);
        var response = videoService.search(request);
        return response;
    }

    @GetMapping(Const.Path.GET + "/{uuid}")
    public ResponseEntity<?> getByUUID(@PathVariable String uuid) throws Exception {
        var response = videoService.getByUUID(uuid);
        return response;
    }
}
