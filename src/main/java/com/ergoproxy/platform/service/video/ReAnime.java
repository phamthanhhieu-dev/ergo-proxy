package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.VideoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReAnime implements VideoRouter {

    @Override
    public String getSource() {
        return Const.Source.REANIME;
    }

    @Override
    public List<VideoDTO> search(String query) {
        List<VideoDTO> response = new ArrayList<>();
        return response;
    }
}
