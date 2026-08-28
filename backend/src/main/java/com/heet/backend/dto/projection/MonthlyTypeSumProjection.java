package com.heet.backend.dto.projection;

import com.heet.backend.enums.TransactionType;

import java.math.BigDecimal;

public interface MonthlyTypeSumProjection {
    Integer getYear();
    Integer getMonth();
    TransactionType getTransactionType();
    BigDecimal getTotalAmount();
}
