package com.ergoproxy.platform.mapping;

import com.ergoproxy.platform.model.dto.subtitle.AttributeDTO;
import com.ergoproxy.platform.model.dto.subtitle.FeatureDetailDTO;
import com.ergoproxy.platform.model.dto.subtitle.SubtitleDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesAttributeDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesFeatureDTO;
import com.ergoproxy.platform.model.dto.subtitle.opensubtitles.OpenSubtitlesGetDetailResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubtitleMapper {

    public SubtitleDTO openSubtitleFeatureDTOToSubtitleDTO(OpenSubtitlesFeatureDTO source) {
        if (source == null) {
            return null;
        }

        SubtitleDTO target = new SubtitleDTO();
        target.setId(source.getId());
        target.setType(source.getAttributes() != null
            ? source.getAttributes().getFeatureType()
            : source.getType());

        if (source.getAttributes() != null) {
            target.setAttributes(openSubtitlesAttributeDTOToAttributeDTO(source.getAttributes()));
        }

        return target;
    }

    private AttributeDTO openSubtitlesAttributeDTOToAttributeDTO(OpenSubtitlesAttributeDTO source) {
        AttributeDTO target = new AttributeDTO();
        target.setFeatureDetails(openSubtitlesAttributeDTOToFeatureDetailDTO(source));
        return target;
    }

    private FeatureDetailDTO openSubtitlesAttributeDTOToFeatureDetailDTO(OpenSubtitlesAttributeDTO source) {
        FeatureDetailDTO target = new FeatureDetailDTO();
        target.setFeatureId(source.getFeatureId());
        target.setFeatureType(source.getFeatureType());
        target.setYear(source.getYear());
        target.setTitle(source.getTitle());
        target.setImdbId(source.getImdbId());
        target.setTmdbId(source.getTmdbId());
        return target;
    }
}
