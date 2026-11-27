package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.common.configuration.properties.CacheProperties;
import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.exception.LogicException;
import com.ergoproxy.platform.mapping.SubtitleMapper;
import com.ergoproxy.platform.model.dto.subtitle.SubtitleDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesAttributeDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesFeatureDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesGetDetailResponseDTO;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import com.ergoproxy.platform.model.dto.video.VideoDTO;
import com.ergoproxy.platform.repository.OpenSubtitlesRepository;
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
    private final OpenSubtitlesRepository openSubtitlesRepository;

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
    public ResponseEntity<?> detail(String uuid, String movieName, Integer year, String type) {
        VideoDTO video;
        if (DataUtils.isNullOrEmpty(uuid)) {
            throw new LogicException("GBUV_000001", "uuid.invalid");
        }
        if (DataUtils.isNullOrEmpty(movieName)) {
            throw new LogicException("GBUV_000002", "movie.name.invalid");
        }
        video = cacheService.hashGet(Const.Source.REDIS, Const.CacheKey.VIDEO_DATA, Const.CacheField.VIDEO + uuid, VideoDTO.class);
        if (video == null) {
            SearchRequestDTO searchRequestDTO = new SearchRequestDTO();
            searchRequestDTO.setSource(Const.Source.REANIME);
            searchRequestDTO.setTitle(movieName);
            searchRequestDTO.setPage(1);
            searchRequestDTO.setSize(20);
            search(searchRequestDTO);
            List<VideoDTO> videos = cacheService.hashValues(Const.Source.REDIS, Const.CacheKey.VIDEO_DATA, VideoDTO.class);
            if (DataUtils.isNullOrEmpty(videos)) {
                throw new LogicException("GBUV_000003", "video.with.movie.name.is.null.or.empty", movieName);
            }
            video = videos.stream().filter(videoDTO -> videoDTO.getTitle().getEnglish().equals(movieName)
                || videoDTO.getTitle().getRomaji().equals(movieName)
                || videoDTO.getTitle().getNativeTitle().equals(movieName))
                .findFirst().orElse(null);
            if (video == null) {
                throw new LogicException("GBUV_000004", "video.with.movie.name.is.null.or.empty", movieName);
            }
        }
        if (!Const.VideoType.LST_VIDEO_TYPE.contains(type)) {
            throw new LogicException("GBUV_00005", "video.type.invalid", type);
        }
        OpenSubtitlesGetDetailResponseDTO openSubtitlesGetDetailResponseDTO = openSubtitlesRepository.getDetails(movieName, year, Const.VideoType.TVSHOW);
        if (openSubtitlesGetDetailResponseDTO != null && !DataUtils.isNullOrEmpty(openSubtitlesGetDetailResponseDTO.getData())) {
            for (OpenSubtitlesFeatureDTO feature : openSubtitlesGetDetailResponseDTO.getData()) {
                if (feature.getAttributes() == null) {
                    continue;
                }
                OpenSubtitlesAttributeDTO attributes = feature.getAttributes();
                boolean isMatched = movieName.equalsIgnoreCase(attributes.getTitle());
                if (!isMatched && attributes.getTitleAka() != null) {
                    for (String title : attributes.getTitleAka()) {
                        if (movieName.equalsIgnoreCase(title)) {
                            isMatched = true;
                            break;
                        }
                    }
                }
                if (!isMatched) {
                    continue;
                }
                if (year != null && !year.equals(attributes.getYear())) {
                    continue;
                }
                video.setImdbId(attributes.getImdbId());
                break;
            }
        }
        return ResponseEntity.ok().body(video);
    }
}
