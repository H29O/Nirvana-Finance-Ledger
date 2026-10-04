package com.heet.backend.service;

import com.heet.backend.entity.Receipt;
import com.heet.backend.repository.ReceiptOcrDataRepository;
import com.heet.backend.repository.ReceiptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service responsible for enforcing the automatic receipt retention policy.
 *
 * Retention Policy:
 * Retains receipts for the current month plus the previous two completed months.
 * Receipts uploaded prior to the first day of the 2nd preceding month are eligible for deletion.
 *
 * Example:
 * If current month is October, receipts uploaded in August, September, and October are retained.
 * Receipts uploaded in July or earlier are deleted.
 * When November arrives, August receipts become eligible for deletion.
 */
@Service
public class ReceiptRetentionService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptRetentionService.class);

    private final ReceiptRepository receiptRepository;
    private final ReceiptOcrDataRepository receiptOcrDataRepository;
    private final ReceiptStorageService receiptStorageService;
    private final String timezoneName;

    private Clock clock = Clock.systemDefaultZone();

    public ReceiptRetentionService(
            ReceiptRepository receiptRepository,
            ReceiptOcrDataRepository receiptOcrDataRepository,
            ReceiptStorageService receiptStorageService,
            @Value("${app.timezone:Asia/Kolkata}") String timezoneName) {
        this.receiptRepository = receiptRepository;
        this.receiptOcrDataRepository = receiptOcrDataRepository;
        this.receiptStorageService = receiptStorageService;
        this.timezoneName = timezoneName;
    }

    /**
     * Override clock for testing date math and rolling transitions.
     */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Returns the configured ZoneId.
     */
    public ZoneId getZoneId() {
        try {
            return ZoneId.of(timezoneName);
        } catch (Exception ex) {
            log.warn("Invalid timezone '{}' configured, falling back to system default.", timezoneName);
            return ZoneId.systemDefault();
        }
    }

    /**
     * Calculates the retention cutoff timestamp based on the current month in the configured timezone.
     * Cutoff = Start of the month 2 months prior to the current month (i.e. Month M-2 at 00:00:00).
     * Any receipt with uploadedAt strictly before this cutoff is eligible for deletion.
     */
    public LocalDateTime calculateRetentionCutoff() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(getZoneId()));
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth oldestRetainedMonth = currentMonth.minusMonths(2);
        return oldestRetainedMonth.atDay(1).atStartOfDay();
    }

    /**
     * Executes automatic deletion of all receipts older than the 2-month retention window.
     *
     * @return count of receipts successfully deleted
     */
    public int deleteExpiredReceipts() {
        LocalDateTime cutoff = calculateRetentionCutoff();
        return deleteReceiptsOlderThan(cutoff);
    }

    /**
     * Deletes all receipts uploaded before the specified cutoff timestamp.
     * Ensures associated OCR data, stored physical files, and database records are removed.
     * Transactions linked to deleted receipts remain completely untouched.
     *
     * @param cutoff the boundary timestamp
     * @return the number of receipts deleted
     */
    public int deleteReceiptsOlderThan(LocalDateTime cutoff) {
        log.info("Starting receipt retention cleanup for receipts uploaded before {}", cutoff);

        List<Receipt> expiredReceipts = receiptRepository.findByUploadedAtBefore(cutoff);
        if (expiredReceipts == null || expiredReceipts.isEmpty()) {
            log.info("Receipt retention cleanup completed: No expired receipts found before {}", cutoff);
            return 0;
        }

        log.info("Found {} receipt(s) eligible for retention deletion.", expiredReceipts.size());

        int deletedCount = 0;
        for (Receipt receipt : expiredReceipts) {
            try {
                deleteSingleReceipt(receipt);
                deletedCount++;
            } catch (Exception ex) {
                log.error("Failed to delete expired receipt ID {}: {}", receipt.getId(), ex.getMessage(), ex);
                // Continue processing other receipts so one problematic record doesn't block the rest
            }
        }

        log.info("Receipt retention cleanup finished: Successfully deleted {} of {} eligible receipts.",
                deletedCount, expiredReceipts.size());
        return deletedCount;
    }

    /**
     * Deletes a single receipt, its associated OCR data, and its physical stored file.
     * Leaves any linked transaction completely intact.
     */
    @Transactional
    public void deleteSingleReceipt(Receipt receipt) {
        Long receiptId = receipt.getId();

        // 1. Delete associated OCR data if present
        try {
            receiptOcrDataRepository.findByReceiptId(receiptId)
                    .ifPresent(receiptOcrDataRepository::delete);
        } catch (Exception ex) {
            log.warn("Could not delete OCR data for receipt ID {}: {}", receiptId, ex.getMessage());
        }

        // 2. Delete physical storage file (safe deleteIfExists)
        if (receipt.getStoragePath() != null) {
            try {
                receiptStorageService.deleteFile(receipt.getStoragePath());
            } catch (Exception ex) {
                log.warn("Could not delete physical file at '{}' for receipt ID {}: {}",
                        receipt.getStoragePath(), receiptId, ex.getMessage());
            }
        }

        // 3. Delete database record
        receiptRepository.delete(receipt);
        log.debug("Successfully deleted expired receipt ID {}", receiptId);
    }

    /**
     * Daily scheduled task executing at the configured low-frequency cron interval.
     */
    @Scheduled(cron = "${app.receipt.cleanup.cron:0 0 2 * * ?}")
    public void runScheduledRetentionCleanup() {
        log.info("Triggered scheduled automatic receipt retention cleanup...");
        try {
            int deleted = deleteExpiredReceipts();
            log.info("Scheduled automatic receipt retention cleanup completed. Total deleted: {}", deleted);
        } catch (Exception ex) {
            log.error("Error during scheduled automatic receipt retention cleanup: {}", ex.getMessage(), ex);
        }
    }
}
