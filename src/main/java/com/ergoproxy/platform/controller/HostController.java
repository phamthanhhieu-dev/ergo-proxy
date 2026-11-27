package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.host.GetEmbedUrlRequestDTO;
import com.ergoproxy.platform.service.host.HostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.SUBTITLE)
public class HostController {
    private final HostService hostService;

    @GetMapping(Const.Path.GET_EMBED_URL +"/{source}/{anilistId}/{episode}")
    public ResponseEntity<?> getEmbedUrl(@PathVariable String source, @PathVariable Long anilistId,
                                         @PathVariable Integer episode) throws Exception {
        GetEmbedUrlRequestDTO requestDTO = new GetEmbedUrlRequestDTO();
        requestDTO.setSource(source);
        requestDTO.setAnilistId(anilistId);
        requestDTO.setEpisode(episode);
        var response = hostService.getEmbedUrl(requestDTO);
        return response;
    }
}
