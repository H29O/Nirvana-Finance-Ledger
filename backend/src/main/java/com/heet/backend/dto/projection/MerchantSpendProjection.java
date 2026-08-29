package com.heet.backend.dto.projection;

import java.math.BigDecimal;

public interface MerchantSpendProjection {
    String getMerchant();
    BigDecimal getTotalAmount();
    Long getTransactionCount();
}
