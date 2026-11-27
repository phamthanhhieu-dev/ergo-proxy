package com.ergoproxy.platform.model.dto.subtitle;

import lombok.Data;

import java.util.List;

@Data
public class AttributeDTO {
    private String subtitleId;
    private String language;
    private Integer downloadCount;
    private Integer newDownloadCount;
    private Boolean hearingImpaired;
    private Boolean hd;
    private Double fps;
    private Integer votes;
    private Double ratings;
    private Boolean fromTrusted;
    private Boolean foreignPartsOnly;
    private String uploadDate;
    private Boolean aiTranslated;
    private Integer nbCd;
    private String slug;
    private Integer thanks;
    private Boolean machineTranslated;
    private String release;
    private String comments;
    private Long legacySubtitleId;
    private Long legacyUploaderId;
    private UploaderDTO uploader;
    private FeatureDetailDTO featureDetails;
    private String url;
    private List<RelatedLinkDTO> relatedLinks;
    private List<SubtitleFileDTO> files;
}
