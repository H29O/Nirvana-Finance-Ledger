package com.heet.backend.dto.response;

import com.heet.backend.enums.OcrStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptOcrResponse {

    private Long id;
    private Long receiptId;
    private OcrStatus status;
    private String rawText;
    private String detectedMerchant;
    private BigDecimal detectedAmount;
    private LocalDate detectedDate;
    private String failureReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
