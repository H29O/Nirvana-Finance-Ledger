package com.heet.backend.service;

import com.heet.backend.dto.response.ReceiptOcrResponse;
import com.heet.backend.entity.Category;
import com.heet.backend.entity.Receipt;
import com.heet.backend.entity.ReceiptOcrData;
import com.heet.backend.entity.User;
import com.heet.backend.enums.OcrStatus;
import com.heet.backend.exception.ReceiptNotFoundException;
import com.heet.backend.exception.ReceiptOcrNotFoundException;
import com.heet.backend.repository.CategoryRepository;
import com.heet.backend.repository.ReceiptOcrDataRepository;
import com.heet.backend.repository.ReceiptRepository;
import com.heet.backend.security.UserPrincipal;
import com.heet.backend.service.ocr.CategoryClassifier;
import com.heet.backend.service.ocr.OcrResult;
import com.heet.backend.service.ocr.OcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptOcrServiceTest {

    @Mock
    private ReceiptOcrDataRepository receiptOcrDataRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryClassifier categoryClassifier;

    @Mock
    private OcrService ocrService;

    @InjectMocks
    private ReceiptOcrService receiptOcrService;

    private User testUser;
    private Receipt sampleReceipt;
    private ReceiptOcrData sampleOcrData;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        UserPrincipal principal = new UserPrincipal(testUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        sampleReceipt = new Receipt();
        sampleReceipt.setId(10L);
        sampleReceipt.setUser(testUser);
        sampleReceipt.setOriginalFileName("receipt.jpg");
        sampleReceipt.setContentType("image/jpeg");
        sampleReceipt.setStoragePath("uploads/receipts/1/sample.jpg");

        sampleOcrData = new ReceiptOcrData();
        sampleOcrData.setId(100L);
        sampleOcrData.setReceipt(sampleReceipt);
        sampleOcrData.setUser(testUser);
        sampleOcrData.setStatus(OcrStatus.COMPLETED);
        sampleOcrData.setRawText("Kwality Dine\nTotal: $50.00\nDate: 2026-08-21");
        sampleOcrData.setDetectedMerchant("Kwality Dine");
        sampleOcrData.setDetectedAmount(new BigDecimal("50.00"));
        sampleOcrData.setDetectedDate(LocalDate.of(2026, 8, 21));
        sampleOcrData.setProcessedAt(LocalDateTime.now());
    }

    @Test
    void processOcr_Success_WithCategorySuggestion() {
        Category healthcareCategory = new Category();
        healthcareCategory.setId(25L);
        healthcareCategory.setName("Healthcare");
        healthcareCategory.setUser(testUser);

        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptOcrDataRepository.findByReceiptIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of(healthcareCategory));
        when(categoryClassifier.suggestCategory(any(), eq("Apollo Pharmacy"), any()))
                .thenReturn(new CategoryClassifier.CategorySuggestion(25L, "Healthcare", "HIGH"));
        when(receiptOcrDataRepository.save(any(ReceiptOcrData.class))).thenAnswer(invocation -> {
            ReceiptOcrData data = invocation.getArgument(0);
            data.setId(100L);
            return data;
        });

        OcrResult mockResult = OcrResult.success(
                "Apollo Pharmacy\nMedicine Total: 450\nNet Payable: ₹450.00",
                "Apollo Pharmacy",
                new BigDecimal("450.00"),
                LocalDate.of(2026, 8, 21)
        );
        when(ocrService.extractTextAndData(any(File.class), eq("image/jpeg"))).thenReturn(mockResult);

        ReceiptOcrResponse response = receiptOcrService.processOcr(10L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getReceiptId());
        assertEquals(OcrStatus.COMPLETED, response.getStatus());
        assertEquals("Apollo Pharmacy", response.getDetectedMerchant());
        assertEquals(new BigDecimal("450.00"), response.getDetectedAmount());
        assertEquals(25L, response.getSuggestedCategoryId());
        assertEquals("Healthcare", response.getSuggestedCategoryName());
        assertEquals("HIGH", response.getConfidenceScore());
        verify(categoryRepository, times(1)).findByUserId(1L);
    }

    @Test
    void processOcr_Success_WithoutCategoryMatch() {
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptOcrDataRepository.findByReceiptIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of());
        when(categoryClassifier.suggestCategory(any(), eq("Kwality Dine"), any())).thenReturn(null);
        when(receiptOcrDataRepository.save(any(ReceiptOcrData.class))).thenAnswer(invocation -> {
            ReceiptOcrData data = invocation.getArgument(0);
            data.setId(100L);
            return data;
        });

        OcrResult mockResult = OcrResult.success(
                "Kwality Dine\nTotal: $50.00\nDate: 2026-08-21",
                "Kwality Dine",
                new BigDecimal("50.00"),
                LocalDate.of(2026, 8, 21)
        );
        when(ocrService.extractTextAndData(any(File.class), eq("image/jpeg"))).thenReturn(mockResult);

        ReceiptOcrResponse response = receiptOcrService.processOcr(10L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertNull(response.getSuggestedCategoryId());
        assertNull(response.getSuggestedCategoryName());
    }

    @Test
    void processOcr_OcrEngineFailure_MarksStatusAsFailed() {
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptOcrDataRepository.findByReceiptIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        when(receiptOcrDataRepository.save(any(ReceiptOcrData.class))).thenAnswer(invocation -> {
            ReceiptOcrData data = invocation.getArgument(0);
            data.setId(100L);
            return data;
        });

        OcrResult mockFailure = OcrResult.failure("PDF OCR is currently not supported.");
        when(ocrService.extractTextAndData(any(File.class), eq("image/jpeg"))).thenReturn(mockFailure);

        ReceiptOcrResponse response = receiptOcrService.processOcr(10L);

        assertNotNull(response);
        assertEquals(OcrStatus.FAILED, response.getStatus());
        assertEquals("PDF OCR is currently not supported.", response.getFailureReason());
        assertNull(response.getDetectedMerchant());
        assertNull(response.getDetectedAmount());
    }

    @Test
    void processOcr_ReceiptNotFound_ThrowsReceiptNotFoundException() {
        when(receiptRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ReceiptNotFoundException.class, () -> receiptOcrService.processOcr(99L));
        verify(ocrService, never()).extractTextAndData(any(), any());
    }

    @Test
    void getOcrData_Success() {
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptOcrDataRepository.findByReceiptIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleOcrData));

        ReceiptOcrResponse response = receiptOcrService.getOcrData(10L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Kwality Dine", response.getDetectedMerchant());
        assertEquals(OcrStatus.COMPLETED, response.getStatus());
    }

    @Test
    void getOcrData_ReceiptNotFound_ThrowsReceiptNotFoundException() {
        when(receiptRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ReceiptNotFoundException.class, () -> receiptOcrService.getOcrData(99L));
    }

    @Test
    void getOcrData_OcrDataNotFound_ThrowsReceiptOcrNotFoundException() {
        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptOcrDataRepository.findByReceiptIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(ReceiptOcrNotFoundException.class, () -> receiptOcrService.getOcrData(10L));
    }

    @Test
    void processOcr_ReprocessFailedOcr_TransitionsToCompleted() {
        ReceiptOcrData failedOcr = new ReceiptOcrData();
        failedOcr.setId(100L);
        failedOcr.setReceipt(sampleReceipt);
        failedOcr.setUser(testUser);
        failedOcr.setStatus(OcrStatus.FAILED);
        failedOcr.setFailureReason("Previous connection timeout");

        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptOcrDataRepository.findByReceiptIdAndUserId(10L, 1L)).thenReturn(Optional.of(failedOcr));
        when(receiptOcrDataRepository.save(any(ReceiptOcrData.class))).thenAnswer(i -> i.getArgument(0));

        OcrResult retryResult = OcrResult.success(
                "Kwality Dine\nTotal: $50.00\nDate: 2026-08-21",
                "Kwality Dine",
                new BigDecimal("50.00"),
                LocalDate.of(2026, 8, 21)
        );
        when(ocrService.extractTextAndData(any(File.class), eq("image/jpeg"))).thenReturn(retryResult);

        ReceiptOcrResponse response = receiptOcrService.processOcr(10L);

        assertNotNull(response);
        assertEquals(OcrStatus.COMPLETED, response.getStatus());
        assertEquals("Kwality Dine", response.getDetectedMerchant());
        assertEquals(new BigDecimal("50.00"), response.getDetectedAmount());
        assertNull(response.getFailureReason());
    }

    @Test
    void updateOcrData_Success_UpdatesAndReturnsCanonicalData() {
        Category groceryCat = new Category();
        groceryCat.setId(30L);
        groceryCat.setName("Groceries");
        groceryCat.setUser(testUser);

        when(receiptRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleReceipt));
        when(receiptOcrDataRepository.findByReceiptIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleOcrData));
        when(categoryRepository.findByIdAndUserId(30L, 1L)).thenReturn(Optional.of(groceryCat));
        when(receiptOcrDataRepository.save(any(ReceiptOcrData.class))).thenAnswer(i -> i.getArgument(0));

        com.heet.backend.dto.request.UpdateReceiptOcrRequest request =
                com.heet.backend.dto.request.UpdateReceiptOcrRequest.builder()
                        .merchant("New Corrected Supermarket")
                        .amount(new BigDecimal("150.00"))
                        .date(LocalDate.of(2026, 9, 15))
                        .categoryId(30L)
                        .build();

        ReceiptOcrResponse response = receiptOcrService.updateOcrData(10L, request);

        assertNotNull(response);
        assertEquals(OcrStatus.COMPLETED, response.getStatus());
        assertEquals("New Corrected Supermarket", response.getDetectedMerchant());
        assertEquals(new BigDecimal("150.00"), response.getDetectedAmount());
        assertEquals(LocalDate.of(2026, 9, 15), response.getDetectedDate());
        assertEquals(30L, response.getSuggestedCategoryId());
        assertEquals("Groceries", response.getSuggestedCategoryName());
        assertNull(response.getFailureReason());
        verify(receiptOcrDataRepository, times(1)).save(any(ReceiptOcrData.class));
    }
}
