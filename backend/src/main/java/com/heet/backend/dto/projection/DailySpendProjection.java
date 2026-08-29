package com.heet.backend.dto.projection;

import java.math.BigDecimal;

public interface DailySpendProjection {
    Integer getYear();
    Integer getMonth();
    Integer getDay();
    BigDecimal getTotalAmount();
    Long getTransactionCount();
}
