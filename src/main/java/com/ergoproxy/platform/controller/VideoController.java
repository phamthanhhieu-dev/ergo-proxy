package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.service.video.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> search(@RequestParam String source, @RequestParam String query, @RequestParam String genre, @RequestParam String season,
                                 @RequestParam String format, @RequestParam String status, @RequestParam String country, @RequestParam String audio,
                                 @RequestParam String rating, @RequestParam Integer year, @RequestParam String sort, @RequestParam String tag,
                                 @RequestParam String character, @RequestParam String staff, @RequestParam String studio, @RequestParam Integer page,
                                 @RequestParam Integer size) throws Exception {
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

    @GetMapping(Const.Path.GET)
    public ResponseEntity<?> get(@RequestParam String source, @RequestParam String query) throws Exception {
        var response = videoService.getDetail(source, query);
        return response;
    }
}
