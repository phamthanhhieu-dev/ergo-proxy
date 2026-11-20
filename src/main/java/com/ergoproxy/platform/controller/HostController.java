package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.service.host.HostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.SUBTITLE)
public class HostController {
    private final HostService hostService;

    @GetMapping(Const.Path.GET_EMBED_URL)
    public String getEmbedUrl(@RequestParam String source, @RequestParam String query) throws Exception {
        var response = hostService.getEmbedUrl(source, query);
        return response;
    }
}
