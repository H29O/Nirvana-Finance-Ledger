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
public class CategoryBreakdownResponse {

    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private Double percentage;
    private Long transactionCount;
}
