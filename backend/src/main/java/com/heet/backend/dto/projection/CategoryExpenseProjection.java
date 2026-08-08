package com.heet.backend.dto.projection;

import java.math.BigDecimal;

public interface CategoryExpenseProjection {
    Long getCategoryId();
    String getCategoryName();
    String getColor();
    String getIcon();
    BigDecimal getTotalAmount();
    Long getTransactionCount();
}
