package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.common.configuration.properties.ReanimeProperties;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.exception.LogicException;
import com.ergoproxy.platform.mapping.VideoMapper;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.VideoDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.reanime.ReanimeSearchResponseDTO;
import com.ergoproxy.platform.repository.ReanimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public ResponseEntity<?> search(SearchRequestDTO searchRequestDTO) {
        if (searchRequestDTO == null) {
            throw new LogicException("VQRA_00001", "anime.query.invalid");
        }
        ReanimeSearchRequestDTO reanimeSearchRequestDTO = videoMapper.searchRequestToReanimeSearchRequest(searchRequestDTO);
        ReanimeSearchResponseDTO reanimeSearchResponseDTO = reanimeRepository.search(reanimeSearchRequestDTO);
        List<VideoDTO> listVideoDTO = videoMapper.reanimeToVideoList(reanimeSearchResponseDTO.getResults());
        Pageable pageable = PageRequest.of(searchRequestDTO.getPage(), searchRequestDTO.getSize());
        Page<VideoDTO> response = new PageImpl<>(listVideoDTO, pageable, reanimeSearchResponseDTO.getTotal());
        return ResponseEntity.ok(response);
    }
}
