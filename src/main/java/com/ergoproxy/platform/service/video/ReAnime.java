package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.common.config.configmapping.ReanimeProperties;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.exception.LogicException;
import com.ergoproxy.platform.mapping.VideoMapper;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.VideoResponseDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchResponseDTO;
import com.ergoproxy.platform.repository.ReanimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReAnime implements VideoRouter {

    private final ReanimeProperties reanimeProperties;
    private final ReanimeRepository reanimeRepository;
    private final VideoMapper videoMapper;

    @Override
    public String getSource() {
        return Const.Source.REANIME;
    }

    @Override
    public List<VideoResponseDTO> search(SearchRequestDTO searchRequestDTO) {
        if (searchRequestDTO == null) {
            throw new LogicException("VQR_00001", "anime.query.invalid");
        }
        ReanimeSearchRequestDTO reanimeSearchRequestDTO = videoMapper.searchRequestToReanimeSearchRequest(searchRequestDTO);
        ReanimeSearchResponseDTO reanimeSearchResponseDTO = reanimeRepository.search(reanimeSearchRequestDTO);
        return videoMapper.reanimeToVideoList(reanimeSearchResponseDTO.getResults());
    }
}
