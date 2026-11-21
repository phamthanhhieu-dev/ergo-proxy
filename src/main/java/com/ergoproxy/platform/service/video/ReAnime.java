package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.common.config.configmapping.ReanimeCfg;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.exception.LogicException;
import com.ergoproxy.platform.mapping.VideoMapper;
import com.ergoproxy.platform.model.dto.video.VideoDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchResponseDTO;
import com.ergoproxy.platform.repository.ReanimeRepository;
import com.ergoproxy.platform.util.DataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReAnime implements VideoRouter {

    private final ReanimeCfg reanimeCfg;
    private final ReanimeRepository reanimeRepository;
    private final VideoMapper videoMapper;

    @Override
    public String getSource() {
        return Const.Source.REANIME;
    }

    @Override
    public List<VideoDTO> search(String query) {
        if (DataUtils.isNullOrEmpty(query)) {
            throw new LogicException("VQR_00001", "anime.query.invalid");
        }
        ReanimeSearchRequestDTO request = DataUtils.parseQuery(query, ReanimeSearchRequestDTO.class);
        request.setLimit(20);
        ReanimeSearchResponseDTO reanimeSearchResponseDTO = reanimeRepository.search(request);
        return videoMapper.reanimeToVideoList(reanimeSearchResponseDTO.getResults());
    }
}
