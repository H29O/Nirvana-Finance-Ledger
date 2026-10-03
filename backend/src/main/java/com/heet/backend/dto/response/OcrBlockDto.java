package com.heet.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrBlockDto {
    private String text;
    private double confidence;

    @JsonProperty("center_x")
    private double centerX;

    @JsonProperty("center_y")
    private double centerY;

    private double width;
    private double height;

    @JsonProperty("line_index")
    private int lineIndex;

    private BoundingBoxDto box;
}
