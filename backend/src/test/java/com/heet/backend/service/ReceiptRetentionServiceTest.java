package com.heet.backend.service;

import com.heet.backend.entity.Receipt;
import com.heet.backend.entity.ReceiptOcrData;
import com.heet.backend.entity.Transaction;
import com.heet.backend.entity.User;
import com.heet.backend.repository.ReceiptOcrDataRepository;
import com.heet.backend.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptRetentionServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ReceiptOcrDataRepository receiptOcrDataRepository;

    @Mock
    private ReceiptStorageService receiptStorageService;

    private ReceiptRetentionService retentionService;

    private final ZoneId zoneId = ZoneId.of("Asia/Kolkata");
    // Simulate current time: 2026-10-15T12:00:00 in Asia/Kolkata
    private final Instant oct15Instant = Instant.parse("2026-10-15T06:30:00Z");

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        retentionService = new ReceiptRetentionService(
                receiptRepository,
                receiptOcrDataRepository,
                receiptStorageService,
                "Asia/Kolkata"
        );
        retentionService.setClock(Clock.fixed(oct15Instant, zoneId));

        user1 = new User();
        user1.setId(1L);
        user1.setName("User One");

        user2 = new User();
        user2.setId(2L);
        user2.setName("User Two");
    }

    @Test
    @DisplayName("Cutoff calculation returns start of month 2 months prior (Aug 1 00:00:00 for October)")
    void calculateRetentionCutoff_ReturnsStartOfMonthTwoMonthsPrior() {
        LocalDateTime cutoff = retentionService.calculateRetentionCutoff();

        // Current month: 2026-10. Oldest retained month: 2026-08. Cutoff: 2026-08-01T00:00:00
        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0, 0), cutoff);
    }

    @Test
    @DisplayName("Receipts within retention window (Aug, Sep, Oct) are preserved")
    void deleteExpiredReceipts_PreservesReceiptsWithinRetentionWindow() {
        LocalDateTime cutoff = retentionService.calculateRetentionCutoff();

        // Repository returns no receipts before cutoff
        when(receiptRepository.findByUploadedAtBefore(cutoff)).thenReturn(List.of());

        int deleted = retentionService.deleteExpiredReceipts();

        assertEquals(0, deleted);
        verify(receiptRepository, never()).delete(any(Receipt.class));
        verify(receiptStorageService, never()).deleteFile(any());
        verify(receiptOcrDataRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Exact retention boundary: 1 sec before cutoff is deleted, at or after cutoff is preserved")
    void deleteExpiredReceipts_HandlesExactRetentionBoundaryCorrectly() {
        LocalDateTime cutoff = retentionService.calculateRetentionCutoff();

        // Cutoff is 2026-08-01T00:00:00.
        // A receipt from 2026-07-31T23:59:59 is before cutoff and eligible.
        Receipt expiredBoundaryReceipt = createReceipt(10L, user1, LocalDateTime.of(2026, 7, 31, 23, 59, 59), "uploads/receipts/1/r1.jpg");

        when(receiptRepository.findByUploadedAtBefore(cutoff))
                .thenReturn(List.of(expiredBoundaryReceipt));

        int deleted = retentionService.deleteExpiredReceipts();

        assertEquals(1, deleted);
        verify(receiptRepository).delete(expiredBoundaryReceipt);
        verify(receiptStorageService).deleteFile("uploads/receipts/1/r1.jpg");
    }

    @Test
    @DisplayName("Receipts older than 2 months are deleted cleanly")
    void deleteExpiredReceipts_DeletesReceiptsOlderThanTwoMonths() {
        LocalDateTime cutoff = retentionService.calculateRetentionCutoff();

        Receipt julyReceipt = createReceipt(1L, user1, LocalDateTime.of(2026, 7, 15, 10, 0), "uploads/receipts/1/july.png");
        Receipt juneReceipt = createReceipt(2L, user1, LocalDateTime.of(2026, 6, 20, 14, 30), "uploads/receipts/1/june.pdf");

        when(receiptRepository.findByUploadedAtBefore(cutoff)).thenReturn(List.of(julyReceipt, juneReceipt));

        int deleted = retentionService.deleteExpiredReceipts();

        assertEquals(2, deleted);
        verify(receiptRepository).delete(julyReceipt);
        verify(receiptRepository).delete(juneReceipt);
        verify(receiptStorageService).deleteFile("uploads/receipts/1/july.png");
        verify(receiptStorageService).deleteFile("uploads/receipts/1/june.pdf");
    }

    @Test
    @DisplayName("Physical file and associated OCR data are cleaned up upon receipt deletion")
    void deleteExpiredReceipts_DeletesPhysicalFileAndOcrData() {
        LocalDateTime cutoff = retentionService.calculateRetentionCutoff();

        Receipt receipt = createReceipt(5L, user1, LocalDateTime.of(2026, 6, 1, 12, 0), "uploads/receipts/1/rec5.png");

        ReceiptOcrData ocrData = new ReceiptOcrData();
        ocrData.setId(105L);
        ocrData.setReceipt(receipt);

        when(receiptRepository.findByUploadedAtBefore(cutoff)).thenReturn(List.of(receipt));
        when(receiptOcrDataRepository.findByReceiptId(5L)).thenReturn(Optional.of(ocrData));

        int deleted = retentionService.deleteExpiredReceipts();

        assertEquals(1, deleted);
        verify(receiptOcrDataRepository).delete(ocrData);
        verify(receiptStorageService).deleteFile("uploads/receipts/1/rec5.png");
        verify(receiptRepository).delete(receipt);
    }

    @Test
    @DisplayName("Linked financial transactions remain completely untouched when receipt is deleted")
    void deleteExpiredReceipts_TransactionsRemainUntouched() {
        LocalDateTime cutoff = retentionService.calculateRetentionCutoff();

        Transaction linkedTx = new Transaction();
        linkedTx.setId(999L);

        Receipt receipt = createReceipt(7L, user1, LocalDateTime.of(2026, 5, 10, 9, 0), "uploads/receipts/1/tx.png");
        receipt.setTransaction(linkedTx);

        when(receiptRepository.findByUploadedAtBefore(cutoff)).thenReturn(List.of(receipt));

        int deleted = retentionService.deleteExpiredReceipts();

        assertEquals(1, deleted);
        // Receipt is deleted
        verify(receiptRepository).delete(receipt);
        // The transaction itself was never modified or deleted
        assertNotNull(receipt.getTransaction());
        assertEquals(999L, receipt.getTransaction().getId());
    }

    @Test
    @DisplayName("Cleanup is idempotent: running repeatedly is safe and performs no extra deletions")
    void deleteExpiredReceipts_IsIdempotent() {
        LocalDateTime cutoff = retentionService.calculateRetentionCutoff();

        Receipt receipt = createReceipt(12L, user1, LocalDateTime.of(2026, 7, 1, 0, 0), "uploads/receipts/1/rep.jpg");

        // First run finds 1 receipt
        when(receiptRepository.findByUploadedAtBefore(cutoff)).thenReturn(new ArrayList<>(List.of(receipt)));

        int firstRun = retentionService.deleteExpiredReceipts();
        assertEquals(1, firstRun);

        // Second run finds 0 receipts
        when(receiptRepository.findByUploadedAtBefore(cutoff)).thenReturn(List.of());

        int secondRun = retentionService.deleteExpiredReceipts();
        assertEquals(0, secondRun);
    }

    @Test
    @DisplayName("Multiple users' expired receipts are handled and deleted correctly")
    void deleteExpiredReceipts_HandlesMultipleUsersReceiptsCorrectly() {
        LocalDateTime cutoff = retentionService.calculateRetentionCutoff();

        Receipt u1Receipt = createReceipt(21L, user1, LocalDateTime.of(2026, 6, 15, 8, 0), "uploads/receipts/1/u1.jpg");
        Receipt u2Receipt = createReceipt(22L, user2, LocalDateTime.of(2026, 7, 20, 18, 0), "uploads/receipts/2/u2.jpg");

        when(receiptRepository.findByUploadedAtBefore(cutoff)).thenReturn(List.of(u1Receipt, u2Receipt));

        int deleted = retentionService.deleteExpiredReceipts();

        assertEquals(2, deleted);
        verify(receiptRepository).delete(u1Receipt);
        verify(receiptRepository).delete(u2Receipt);
        verify(receiptStorageService).deleteFile("uploads/receipts/1/u1.jpg");
        verify(receiptStorageService).deleteFile("uploads/receipts/2/u2.jpg");
    }

    @Test
    @DisplayName("Resilience: One failure during deletion does not abort deletion of other receipts")
    void deleteExpiredReceipts_OneFailureDoesNotPreventOtherReceiptsFromBeingDeleted() {
        LocalDateTime cutoff = retentionService.calculateRetentionCutoff();

        Receipt failingReceipt = createReceipt(31L, user1, LocalDateTime.of(2026, 7, 10, 8, 0), "uploads/receipts/1/failing.jpg");
        Receipt goodReceipt = createReceipt(32L, user2, LocalDateTime.of(2026, 7, 11, 8, 0), "uploads/receipts/2/good.jpg");

        when(receiptRepository.findByUploadedAtBefore(cutoff)).thenReturn(List.of(failingReceipt, goodReceipt));
        // Mock exception on first receipt database deletion
        doThrow(new RuntimeException("Simulated database lock error"))
                .when(receiptRepository).delete(failingReceipt);

        int deleted = retentionService.deleteExpiredReceipts();

        // 1 failed, 1 succeeded
        assertEquals(1, deleted);
        verify(receiptRepository).delete(goodReceipt);
        verify(receiptStorageService).deleteFile("uploads/receipts/2/good.jpg");
    }

    @Test
    @DisplayName("Month-boundary rolling behavior: August receipt preserved in October, deleted when November arrives")
    void deleteExpiredReceipts_MonthBoundaryBehavior() {
        Receipt augustReceipt = createReceipt(40L, user1, LocalDateTime.of(2026, 8, 15, 11, 0), "uploads/receipts/1/august.jpg");

        // 1. In October 2026:
        retentionService.setClock(Clock.fixed(Instant.parse("2026-10-25T10:00:00Z"), zoneId));
        LocalDateTime octCutoff = retentionService.calculateRetentionCutoff(); // 2026-08-01T00:00:00

        // August 15 is NOT before August 1 -> preserved!
        assertFalse(augustReceipt.getUploadedAt().isBefore(octCutoff));

        // 2. In November 2026:
        retentionService.setClock(Clock.fixed(Instant.parse("2026-11-01T06:00:00Z"), zoneId));
        LocalDateTime novCutoff = retentionService.calculateRetentionCutoff(); // 2026-09-01T00:00:00

        // August 15 IS before September 1 -> eligible for deletion!
        assertTrue(augustReceipt.getUploadedAt().isBefore(novCutoff));

        when(receiptRepository.findByUploadedAtBefore(novCutoff)).thenReturn(List.of(augustReceipt));

        int deletedInNov = retentionService.deleteExpiredReceipts();
        assertEquals(1, deletedInNov);
        verify(receiptRepository).delete(augustReceipt);
        verify(receiptStorageService).deleteFile("uploads/receipts/1/august.jpg");
    }

    private Receipt createReceipt(Long id, User user, LocalDateTime uploadedAt, String storagePath) {
        Receipt receipt = new Receipt();
        receipt.setId(id);
        receipt.setUser(user);
        receipt.setOriginalFileName("test.jpg");
        receipt.setStoredFileName("stored.jpg");
        receipt.setContentType("image/jpeg");
        receipt.setFileSize(1024L);
        receipt.setStoragePath(storagePath);
        receipt.setUploadedAt(uploadedAt);
        return receipt;
    }
}
