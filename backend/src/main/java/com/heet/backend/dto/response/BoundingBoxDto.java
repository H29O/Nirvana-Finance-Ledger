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
public class BoundingBoxDto {
    private List<List<Double>> points;

    @JsonProperty("top_left")
    private List<Double> topLeft;

    @JsonProperty("bottom_right")
    private List<Double> bottomRight;

    @JsonProperty("center_x")
    private double centerX;

    @JsonProperty("center_y")
    private double centerY;

    private double width;
    private double height;
}
