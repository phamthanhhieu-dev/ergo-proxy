package com.ergoproxy.platform.controller;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.service.EpisodeBundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequiredArgsConstructor
@RequestMapping(Const.Path.API + Const.Version.V1 + Const.Path.BUNDLE)
public class BundleController {

    private final EpisodeBundleService episodeBundleService;

    @PostMapping(value = "/native", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String payload, @RequestParam(defaultValue = Const.Common.REANIME) String source)
        throws Exception {
        ResponseEntity<StreamingResponseBody> response = episodeBundleService.download(payload, source);
        return response;
    }
}
