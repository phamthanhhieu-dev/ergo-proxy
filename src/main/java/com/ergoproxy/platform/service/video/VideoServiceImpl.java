package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.common.configuration.properties.CacheProperties;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.exception.LogicException;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.VideoDTO;
import com.ergoproxy.platform.service.cache.CacheService;
import com.ergoproxy.platform.util.DataUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final List<VideoRouter> contracts;
    private final CacheProperties cacheProperties;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private VideoRouter getSource(String source) {
        return contracts.stream()
            .filter(contract -> contract.getSource().equals(source))
            .findFirst()
            .orElseThrow(() -> new LogicException("GSV_00002", "video.source.unsupported", source));
    }

    @Override
    public ResponseEntity<?> search(SearchRequestDTO searchRequestDTO) {
        List<VideoDTO> videos = new ArrayList<>();
        //kiem tra truoc xong cache, neu da ton tai thi lay trong cache
        String requestStr = objectMapper.writeValueAsString(searchRequestDTO);
        String searchKey = Const.CacheField.SEARCH + UUID.nameUUIDFromBytes((searchRequestDTO.getSource() + Const.Formatter.SPLIT_STR_COLON + requestStr).getBytes(StandardCharsets.UTF_8));
        List<String> uuids = cacheService.hashGet(Const.Source.REDIS, Const.CacheKey.SEARCH_UUIDS, searchKey, List.class);
        if (!DataUtils.isNullOrEmpty(uuids)) {
            //neu ton tai, thuc hien lay thong tin da luu trong cache dua tren uuid
            for (String uuid : uuids) {
                VideoDTO videoDTO = cacheService.hashGet(Const.Source.REDIS, Const.CacheKey.VIDEO_DATA, Const.CacheField.VIDEO + uuid, VideoDTO.class);
                videos.add(videoDTO);
            }
        } else {
            //neu khong co cache thi thuc hien goi dich vu khac lay thong tin
            videos = getSource(searchRequestDTO.getSource()).search(searchRequestDTO);
            //Luu thong tin vao cache neu chua ton tai trong cache
            List<String> videoUUIDs = new ArrayList<>();
            for (VideoDTO videoDTO : videos) {
                String videoUUID = UUID.nameUUIDFromBytes(videoDTO.getTitle().getRomaji().trim().toLowerCase().getBytes(StandardCharsets.UTF_8)).toString();
                videoDTO.setUUID(videoUUID);
                videoUUIDs.add(videoUUID);
                VideoDTO cachedVideo = cacheService.hashGet(Const.Source.REDIS, Const.CacheKey.VIDEO_DATA, Const.CacheField.VIDEO + videoUUID, VideoDTO.class);
                if (cachedVideo == null) {
                    cacheService.hashSet(Const.Source.REDIS, Const.CacheKey.VIDEO_DATA, Const.CacheField.VIDEO + videoUUID, videoDTO, cacheProperties.getVideoCacheTtl());
                }
            }
            cacheService.hashSet(Const.Source.REDIS, Const.CacheKey.SEARCH_UUIDS, searchKey, videoUUIDs, cacheProperties.getVideoCacheTtl());
        }
        return ResponseEntity.ok(new PageImpl<>(videos));
    }

    @Override
    public ResponseEntity<?> getByUUID(String uuid) {
        if (DataUtils.isNullOrEmpty(uuid)) {
            throw new LogicException("GBUV_000001", "uuid.invalid");
        }
        List<VideoDTO> videos = cacheService.hashValues(Const.Source.REDIS, Const.CacheKey.VIDEO_DATA, VideoDTO.class);
        var response = videos.stream().filter(videoDTO -> videoDTO.getUUID().equals(uuid)).findFirst();
        if (DataUtils.isNullOrEmpty(response)) {
            throw new LogicException("GBUV_000002", "video.with.uuid.is.null.or.empty", uuid);
        }
        return ResponseEntity.ok().body(response);
    }
}
