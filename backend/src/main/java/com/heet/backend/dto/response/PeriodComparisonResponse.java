package com.heet.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodComparisonResponse {

    private PeriodSummaryResponse currentPeriod;
    private PeriodSummaryResponse previousPeriod;
    private Double incomeChangePercentage;
    private Double expenseChangePercentage;
    private Double savingsChangePercentage;
}
