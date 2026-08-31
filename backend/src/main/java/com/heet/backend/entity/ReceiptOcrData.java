package com.heet.backend.entity;

import com.heet.backend.enums.OcrStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "receipt_ocr_data")
public class ReceiptOcrData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false, unique = true)
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OcrStatus status;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    private String detectedMerchant;

    private BigDecimal detectedAmount;

    private LocalDate detectedDate;

    private Long suggestedCategoryId;

    private String suggestedCategoryName;

    private String confidenceScore;

    private String failureReason;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
