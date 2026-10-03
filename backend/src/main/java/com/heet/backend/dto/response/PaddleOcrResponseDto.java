package com.heet.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaddleOcrResponseDto {
    private boolean success;

    @JsonProperty("raw_text")
    private String rawText;

    private List<OcrBlockDto> blocks;
    private List<String> lines;

    @JsonProperty("page_width")
    private Integer pageWidth;

    @JsonProperty("page_height")
    private Integer pageHeight;

    @JsonProperty("processing_time_ms")
    private Double processingTimeMs;

    private String error;
}
