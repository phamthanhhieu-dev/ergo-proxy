package com.ergoproxy.platform.service.subtitle;

import com.ergoproxy.platform.common.config.configmapping.OpenSubtitleCfg;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.SubtitleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenSubtitle implements SubtitleRouter {
    private final OpenSubtitleCfg openSubtitleCfg;

    @Override
    public String getSource() {
        return Const.Source.OPEN_SUBTITLE;
    }

    @Override
    public List<SubtitleDTO> search(String query) {
        List<SubtitleDTO> response = new ArrayList<>();
        return response;
    }
}
