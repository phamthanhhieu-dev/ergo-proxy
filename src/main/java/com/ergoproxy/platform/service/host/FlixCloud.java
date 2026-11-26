package com.ergoproxy.platform.service.host;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.exception.LogicException;
import com.ergoproxy.platform.mapping.HostMapper;
import com.ergoproxy.platform.model.dto.host.GetEmbedUrlRequestDTO;
import com.ergoproxy.platform.model.dto.host.HostDTO;
import com.ergoproxy.platform.model.dto.host.reanime.ReanimeHostResponseDTO;
import com.ergoproxy.platform.repository.ReanimeRepository;
import com.ergoproxy.platform.util.DataUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlixCloud implements HostRouter {

    private final ReanimeRepository reanimeRepository;
    private final HostMapper hostMapper;

    @Override
    public String getSource() {
        return Const.Source.FLIX_CLOUD;
    }

    @Override
    public List<HostDTO> getEmbedUrl(GetEmbedUrlRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new LogicException("GEURA_00001", "request.invalid");
        }
        Long anilistId = requestDTO.getAnilistId();
        if (anilistId == null) {
            throw new LogicException("GEURA_00002", "anilist.id.invalid");
        }
        Integer episode = requestDTO.getEpisode();
        if (episode == null) {
            throw new LogicException("GEURA_00003", "episode.invalid");
        }
        ReanimeHostResponseDTO reanimeHostResponseDTO = reanimeRepository.getEmbedUrl(anilistId, episode);
        List<HostDTO> listHostDTO = hostMapper.reanimeHostToHostList(reanimeHostResponseDTO.getServers());
        listHostDTO.forEach(hostDTO ->
        {
            hostDTO.setSource(getSource());
            hostDTO.setEpisode(episode);
        });
        return listHostDTO;
    }
}
