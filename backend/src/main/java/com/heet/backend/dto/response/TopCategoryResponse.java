package com.heet.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCategoryResponse {

    private Long categoryId;
    private String categoryName;
    private String color;
    private String icon;
    private BigDecimal totalAmount;
    private Long transactionCount;
}
