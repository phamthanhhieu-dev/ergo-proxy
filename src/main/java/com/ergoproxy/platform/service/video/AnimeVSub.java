package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.VideoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnimeVSub implements VideoRouter {

    @Override
    public String getSource() {
        return Const.Source.ANIMEVSUB;
    }

    @Override
    public List<VideoDTO> search(SearchRequestDTO searchRequestDTO) {
        List<VideoDTO> listVideoDTO = new ArrayList<>();
        return listVideoDTO;
    }
}
