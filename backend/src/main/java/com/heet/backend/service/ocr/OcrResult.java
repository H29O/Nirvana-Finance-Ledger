package com.heet.backend.service.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {

    private String rawText;
    private String detectedMerchant;
    private BigDecimal detectedAmount;
    private LocalDate detectedDate;
    private boolean success;
    private String failureReason;

    public static OcrResult success(String rawText, String merchant, BigDecimal amount, LocalDate date) {
        return OcrResult.builder()
                .rawText(rawText)
                .detectedMerchant(merchant)
                .detectedAmount(amount)
                .detectedDate(date)
                .success(true)
                .build();
    }

    public static OcrResult failure(String reason) {
        return OcrResult.builder()
                .success(false)
                .failureReason(reason)
                .build();
    }
}
