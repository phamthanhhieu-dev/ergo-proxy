package com.ergoproxy.platform.model.dto.bundle;

import com.ergoproxy.platform.model.dto.original.OriginalDownloadResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreparedEpisodeBundleDTO {

    private EpisodeBundleRequestDTO request;
    private OriginalDownloadResponseDTO first;
    private String folder;
    private String source;
}
