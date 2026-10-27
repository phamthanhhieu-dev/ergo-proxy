package com.ergoproxy.platform.dto.subtitle;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleSaveRequestDTO {

    @Min(value = 1, message = "{subtitle.save.file.id.min.invalid}")
    private long fileId;

    @jakarta.validation.constraints.NotNull
    private UUID jobId;

    @jakarta.validation.constraints.NotBlank
    private String videoId;

    @jakarta.validation.constraints.Pattern(regexp = "vi|en|ja")
    @jakarta.validation.constraints.NotNull
    private String language;
}
