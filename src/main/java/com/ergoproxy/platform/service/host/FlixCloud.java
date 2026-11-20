package com.ergoproxy.platform.service.host;

import com.ergoproxy.platform.common.constant.Const;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlixCloud implements HostRouter {

    @Override
    public String getSource() {
        return Const.Source.FLIX_CLOUD;
    }

    @Override
    public String getEmbedUrl(String test) {
        return "";
    }
}
