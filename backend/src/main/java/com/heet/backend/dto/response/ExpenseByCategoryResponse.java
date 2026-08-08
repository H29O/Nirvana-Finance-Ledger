package com.heet.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseByCategoryResponse {

    private BigDecimal totalExpense;
    private List<CategoryExpenseItem> categories;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryExpenseItem {
        private Long categoryId;
        private String categoryName;
        private String color;
        private String icon;
        private BigDecimal totalAmount;
    }
}
