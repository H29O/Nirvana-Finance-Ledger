package com.heet.backend.dto.projection;

import com.heet.backend.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentMethodSumProjection {
    PaymentMethod getPaymentMethod();
    BigDecimal getTotalAmount();
    Long getTransactionCount();
}
