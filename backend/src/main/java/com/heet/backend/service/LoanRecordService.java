package com.heet.backend.service;

import com.heet.backend.dto.request.CreateLoanRecordRequest;
import com.heet.backend.dto.request.LoanPaymentRequest;
import com.heet.backend.dto.request.UpdateLoanRecordRequest;
import com.heet.backend.dto.response.LoanRecordResponse;
import com.heet.backend.entity.LoanRecord;
import com.heet.backend.entity.User;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import com.heet.backend.exception.InvalidLoanPaymentException;
import com.heet.backend.exception.LoanRecordNotFoundException;
import com.heet.backend.repository.LoanRecordRepository;
import com.heet.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class LoanRecordService {

    private final LoanRecordRepository loanRecordRepository;

    public LoanRecordService(LoanRecordRepository loanRecordRepository) {
        this.loanRecordRepository = loanRecordRepository;
    }

    @Transactional
    public LoanRecordResponse createLoanRecord(CreateLoanRecordRequest request) {
        User user = SecurityUtils.getCurrentUser();

        LoanRecord loanRecord = new LoanRecord();
        loanRecord.setUser(user);
        loanRecord.setType(request.getType());
        loanRecord.setPersonName(request.getPersonName());
        loanRecord.setDescription(request.getDescription());
        loanRecord.setOriginalAmount(request.getOriginalAmount());
        loanRecord.setRemainingAmount(request.getOriginalAmount());
        loanRecord.setStatus(LoanStatus.ACTIVE);
        loanRecord.setDueDate(request.getDueDate());

        LoanRecord savedRecord = loanRecordRepository.save(loanRecord);
        return mapToResponse(savedRecord);
    }

    @Transactional(readOnly = true)
    public Page<LoanRecordResponse> getLoanRecords(LoanType type, LoanStatus status, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();

        Page<LoanRecord> page;
        if (type != null && status != null) {
            page = loanRecordRepository.findByUserIdAndTypeAndStatus(userId, type, status, pageable);
        } else if (type != null) {
            page = loanRecordRepository.findByUserIdAndType(userId, type, pageable);
        } else if (status != null) {
            page = loanRecordRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            page = loanRecordRepository.findByUserId(userId, pageable);
        }

        return page.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public LoanRecordResponse getLoanRecordById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        LoanRecord loanRecord = findLoanRecordByIdAndUser(id, userId);
        return mapToResponse(loanRecord);
    }

    @Transactional
    public LoanRecordResponse updateLoanRecord(Long id, UpdateLoanRecordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        LoanRecord loanRecord = findLoanRecordByIdAndUser(id, userId);

        loanRecord.setType(request.getType());
        loanRecord.setPersonName(request.getPersonName());
        loanRecord.setDescription(request.getDescription());
        loanRecord.setDueDate(request.getDueDate());

        LoanRecord savedRecord = loanRecordRepository.save(loanRecord);
        return mapToResponse(savedRecord);
    }

    @Transactional
    public void deleteLoanRecord(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        LoanRecord loanRecord = findLoanRecordByIdAndUser(id, userId);
        loanRecordRepository.delete(loanRecord);
    }

    @Transactional
    public LoanRecordResponse recordPayment(Long id, LoanPaymentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        LoanRecord loanRecord = findLoanRecordByIdAndUser(id, userId);

        if (loanRecord.getStatus() == LoanStatus.SETTLED) {
            throw new InvalidLoanPaymentException("Cannot record payment against a settled loan record");
        }

        BigDecimal paymentAmount = request.getAmount();
        if (paymentAmount.compareTo(loanRecord.getRemainingAmount()) > 0) {
            throw new InvalidLoanPaymentException("Payment amount cannot exceed remaining amount");
        }

        BigDecimal newRemainingAmount = loanRecord.getRemainingAmount().subtract(paymentAmount);
        loanRecord.setRemainingAmount(newRemainingAmount);

        if (newRemainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            loanRecord.setStatus(LoanStatus.SETTLED);
        } else {
            loanRecord.setStatus(LoanStatus.ACTIVE);
        }

        LoanRecord savedRecord = loanRecordRepository.save(loanRecord);
        return mapToResponse(savedRecord);
    }

    private LoanRecord findLoanRecordByIdAndUser(Long id, Long userId) {
        return loanRecordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new LoanRecordNotFoundException(id));
    }

    private LoanRecordResponse mapToResponse(LoanRecord loanRecord) {
        return LoanRecordResponse.builder()
                .id(loanRecord.getId())
                .type(loanRecord.getType())
                .personName(loanRecord.getPersonName())
                .description(loanRecord.getDescription())
                .originalAmount(loanRecord.getOriginalAmount())
                .remainingAmount(loanRecord.getRemainingAmount())
                .status(loanRecord.getStatus())
                .dueDate(loanRecord.getDueDate())
                .createdAt(loanRecord.getCreatedAt())
                .updatedAt(loanRecord.getUpdatedAt())
                .build();
    }
}
