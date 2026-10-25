package com.heet.backend.service;

import com.heet.backend.dto.request.AddLoanPrincipalRequest;
import com.heet.backend.dto.request.CreateLoanRecordRequest;
import com.heet.backend.dto.request.LoanPaymentRequest;
import com.heet.backend.dto.request.UpdateLoanRecordRequest;
import com.heet.backend.dto.response.LoanEventResponse;
import com.heet.backend.dto.response.LoanRecordResponse;
import com.heet.backend.entity.LoanEvent;
import com.heet.backend.entity.LoanRecord;
import com.heet.backend.entity.User;
import com.heet.backend.enums.LoanEventType;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import com.heet.backend.exception.InvalidLoanPaymentException;
import com.heet.backend.exception.LoanRecordNotFoundException;
import com.heet.backend.repository.LoanEventRepository;
import com.heet.backend.repository.LoanRecordRepository;
import com.heet.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class LoanRecordService {

    private final LoanRecordRepository loanRecordRepository;
    private final LoanEventRepository loanEventRepository;

    public LoanRecordService(LoanRecordRepository loanRecordRepository, LoanEventRepository loanEventRepository) {
        this.loanRecordRepository = loanRecordRepository;
        this.loanEventRepository = loanEventRepository;
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
        loanRecord.setAdditionalAmount(BigDecimal.ZERO);
        loanRecord.setRemainingAmount(request.getOriginalAmount());
        loanRecord.setStatus(LoanStatus.ACTIVE);
        loanRecord.setDueDate(request.getDueDate());

        LoanRecord savedRecord = loanRecordRepository.save(loanRecord);

        LoanEvent initialEvent = new LoanEvent();
        initialEvent.setLoanRecord(savedRecord);
        initialEvent.setUser(user);
        initialEvent.setEventType(LoanEventType.INITIAL_PRINCIPAL);
        initialEvent.setAmount(savedRecord.getOriginalAmount());
        initialEvent.setNotes("Opening principal");
        initialEvent.setEventDate(savedRecord.getCreatedAt() != null ? savedRecord.getCreatedAt() : LocalDateTime.now());
        loanEventRepository.save(initialEvent);

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

        if (request.getOriginalAmount() != null && request.getOriginalAmount().compareTo(loanRecord.getOriginalAmount()) != 0) {
            BigDecimal oldOriginal = loanRecord.getOriginalAmount();
            BigDecimal newOriginal = request.getOriginalAmount();
            BigDecimal additional = loanRecord.getAdditionalAmount() != null ? loanRecord.getAdditionalAmount() : BigDecimal.ZERO;
            BigDecimal currentTotalRepaid = loanRecord.getTotalRepaid();
            BigDecimal newTotalPrincipal = newOriginal.add(additional);

            if (newTotalPrincipal.compareTo(currentTotalRepaid) < 0) {
                throw new InvalidLoanPaymentException(
                        "Updated principal cannot be less than total amount already repaid (" + currentTotalRepaid + ")");
            }

            BigDecimal newRemaining = newTotalPrincipal.subtract(currentTotalRepaid);
            loanRecord.setOriginalAmount(newOriginal);
            loanRecord.setRemainingAmount(newRemaining);

            if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
                loanRecord.setStatus(LoanStatus.SETTLED);
            } else {
                loanRecord.setStatus(LoanStatus.ACTIVE);
            }

            LoanEvent adjEvent = new LoanEvent();
            adjEvent.setLoanRecord(loanRecord);
            adjEvent.setUser(loanRecord.getUser());
            adjEvent.setEventType(LoanEventType.PRINCIPAL_ADJUSTMENT);
            adjEvent.setAmount(newOriginal.subtract(oldOriginal));
            adjEvent.setNotes("Principal adjusted from " + oldOriginal + " to " + newOriginal);
            adjEvent.setEventDate(LocalDateTime.now());
            loanEventRepository.save(adjEvent);
        }

        loanRecord.setType(request.getType());
        loanRecord.setPersonName(request.getPersonName());
        loanRecord.setDescription(request.getDescription());
        loanRecord.setDueDate(request.getDueDate());

        LoanRecord savedRecord = loanRecordRepository.save(loanRecord);
        return mapToResponse(savedRecord);
    }

    @Transactional
    public LoanRecordResponse addMoreMoney(Long id, AddLoanPrincipalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        LoanRecord loanRecord = findLoanRecordByIdAndUser(id, userId);

        BigDecimal addAmount = request.getAmount();
        if (addAmount == null || addAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidLoanPaymentException("Additional principal amount must be greater than zero");
        }

        BigDecimal currentAdditional = loanRecord.getAdditionalAmount() != null ? loanRecord.getAdditionalAmount() : BigDecimal.ZERO;
        loanRecord.setAdditionalAmount(currentAdditional.add(addAmount));
        loanRecord.setRemainingAmount(loanRecord.getRemainingAmount().add(addAmount));

        if (loanRecord.getStatus() == LoanStatus.SETTLED) {
            loanRecord.setStatus(LoanStatus.ACTIVE);
        }

        LoanRecord savedRecord = loanRecordRepository.save(loanRecord);

        LoanEvent addEvent = new LoanEvent();
        addEvent.setLoanRecord(savedRecord);
        addEvent.setUser(savedRecord.getUser());
        addEvent.setEventType(LoanEventType.ADDITIONAL_PRINCIPAL);
        addEvent.setAmount(addAmount);
        addEvent.setNotes(request.getNotes() != null && !request.getNotes().isBlank() ? request.getNotes() : "Additional principal added");
        LocalDateTime eventDate = request.getDate() != null ? request.getDate().atStartOfDay() : LocalDateTime.now();
        addEvent.setEventDate(eventDate);
        loanEventRepository.save(addEvent);

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
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidLoanPaymentException("Payment amount must be greater than zero");
        }

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

        LoanEvent paymentEvent = new LoanEvent();
        paymentEvent.setLoanRecord(savedRecord);
        paymentEvent.setUser(savedRecord.getUser());
        paymentEvent.setEventType(LoanEventType.PAYMENT);
        paymentEvent.setAmount(paymentAmount);
        paymentEvent.setNotes(request.getNotes() != null && !request.getNotes().isBlank() ? request.getNotes() : "Loan repayment");
        LocalDateTime eventDate = request.getPaymentDate() != null ? request.getPaymentDate().atStartOfDay() : LocalDateTime.now();
        paymentEvent.setEventDate(eventDate);
        loanEventRepository.save(paymentEvent);

        return mapToResponse(savedRecord);
    }

    private LoanRecord findLoanRecordByIdAndUser(Long id, Long userId) {
        return loanRecordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new LoanRecordNotFoundException(id));
    }

    private LoanRecordResponse mapToResponse(LoanRecord loanRecord) {
        List<LoanEvent> events = loanEventRepository
                .findByLoanRecordIdAndUserIdOrderByEventDateAscIdAsc(loanRecord.getId(), loanRecord.getUser().getId());

        List<LoanEventResponse> eventResponses = events != null ? events.stream()
                .map(e -> LoanEventResponse.builder()
                        .id(e.getId())
                        .eventType(e.getEventType())
                        .amount(e.getAmount())
                        .notes(e.getNotes())
                        .eventDate(e.getEventDate())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList() : Collections.emptyList();

        return LoanRecordResponse.builder()
                .id(loanRecord.getId())
                .type(loanRecord.getType())
                .personName(loanRecord.getPersonName())
                .description(loanRecord.getDescription())
                .originalAmount(loanRecord.getOriginalAmount())
                .additionalAmount(loanRecord.getAdditionalAmount() != null ? loanRecord.getAdditionalAmount() : BigDecimal.ZERO)
                .totalPrincipal(loanRecord.getTotalPrincipal())
                .totalRepaid(loanRecord.getTotalRepaid())
                .remainingAmount(loanRecord.getRemainingAmount())
                .status(loanRecord.getStatus())
                .dueDate(loanRecord.getDueDate())
                .createdAt(loanRecord.getCreatedAt())
                .updatedAt(loanRecord.getUpdatedAt())
                .events(eventResponses)
                .build();
    }
}
