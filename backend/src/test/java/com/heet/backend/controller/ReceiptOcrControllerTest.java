package com.heet.backend.controller;

import com.heet.backend.dto.response.ReceiptOcrResponse;
import com.heet.backend.enums.OcrStatus;
import com.heet.backend.service.ReceiptOcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptOcrControllerTest {

    @Mock
    private ReceiptOcrService receiptOcrService;

    @InjectMocks
    private ReceiptOcrController receiptOcrController;

    private ReceiptOcrResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = ReceiptOcrResponse.builder()
                .id(100L)
                .receiptId(10L)
                .status(OcrStatus.COMPLETED)
                .rawText("Kwality Dine\nTotal: $50.00\nDate: 2026-08-21")
                .detectedMerchant("Kwality Dine")
                .detectedAmount(new BigDecimal("50.00"))
                .detectedDate(LocalDate.of(2026, 8, 21))
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processOcr_Returns200OK() {
        when(receiptOcrService.processOcr(10L)).thenReturn(sampleResponse);

        ResponseEntity<ReceiptOcrResponse> result = receiptOcrController.processOcr(10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(100L, result.getBody().getId());
        assertEquals("Kwality Dine", result.getBody().getDetectedMerchant());
    }

    @Test
    void getOcrData_Returns200OK() {
        when(receiptOcrService.getOcrData(10L)).thenReturn(sampleResponse);

        ResponseEntity<ReceiptOcrResponse> result = receiptOcrController.getOcrData(10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(100L, result.getBody().getId());
    }

    @Test
    void updateOcrData_Returns200OK() {
        com.heet.backend.dto.request.UpdateReceiptOcrRequest request =
                com.heet.backend.dto.request.UpdateReceiptOcrRequest.builder()
                        .merchant("Corrected Merchant")
                        .amount(new BigDecimal("150.00"))
                        .date(LocalDate.of(2026, 9, 1))
                        .build();

        ReceiptOcrResponse updatedResponse = ReceiptOcrResponse.builder()
                .id(100L)
                .receiptId(10L)
                .status(OcrStatus.COMPLETED)
                .detectedMerchant("Corrected Merchant")
                .detectedAmount(new BigDecimal("150.00"))
                .detectedDate(LocalDate.of(2026, 9, 1))
                .build();

        when(receiptOcrService.updateOcrData(eq(10L), any())).thenReturn(updatedResponse);

        ResponseEntity<ReceiptOcrResponse> result = receiptOcrController.updateOcrData(10L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Corrected Merchant", result.getBody().getDetectedMerchant());
        assertEquals(new BigDecimal("150.00"), result.getBody().getDetectedAmount());
    }
}
