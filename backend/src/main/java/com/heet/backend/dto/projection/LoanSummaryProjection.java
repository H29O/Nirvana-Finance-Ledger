package com.heet.backend.dto.projection;

import com.heet.backend.enums.LoanType;

import java.math.BigDecimal;

public interface LoanSummaryProjection {
    LoanType getType();
    BigDecimal getTotalRemainingAmount();
    Long getCount();
}
