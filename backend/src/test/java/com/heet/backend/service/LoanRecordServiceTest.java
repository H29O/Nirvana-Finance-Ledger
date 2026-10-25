package com.heet.backend.service;

import com.heet.backend.dto.request.AddLoanPrincipalRequest;
import com.heet.backend.dto.request.CreateLoanRecordRequest;
import com.heet.backend.dto.request.LoanPaymentRequest;
import com.heet.backend.dto.request.UpdateLoanRecordRequest;
import com.heet.backend.dto.response.LoanRecordResponse;
import com.heet.backend.entity.LoanEvent;
import com.heet.backend.entity.LoanRecord;
import com.heet.backend.entity.User;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import com.heet.backend.exception.InvalidLoanPaymentException;
import com.heet.backend.exception.LoanRecordNotFoundException;
import com.heet.backend.repository.LoanEventRepository;
import com.heet.backend.repository.LoanRecordRepository;
import com.heet.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanRecordServiceTest {

    @Mock
    private LoanRecordRepository loanRecordRepository;

    @Mock
    private LoanEventRepository loanEventRepository;

    @InjectMocks
    private LoanRecordService loanRecordService;

    private User testUser;
    private LoanRecord sampleRecord;

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

        sampleRecord = new LoanRecord();
        sampleRecord.setId(10L);
        sampleRecord.setUser(testUser);
        sampleRecord.setType(LoanType.LENT);
        sampleRecord.setPersonName("John Doe");
        sampleRecord.setDescription("Lunch loan");
        sampleRecord.setOriginalAmount(BigDecimal.valueOf(100.00));
        sampleRecord.setAdditionalAmount(BigDecimal.ZERO);
        sampleRecord.setRemainingAmount(BigDecimal.valueOf(100.00));
        sampleRecord.setStatus(LoanStatus.ACTIVE);
        sampleRecord.setDueDate(LocalDate.of(2026, 12, 31));
    }

    @Test
    void createLoanRecord_Success() {
        CreateLoanRecordRequest request = new CreateLoanRecordRequest(
                LoanType.LENT,
                "John Doe",
                "Lunch loan",
                BigDecimal.valueOf(100.00),
                LocalDate.of(2026, 12, 31)
        );

        when(loanRecordRepository.save(any(LoanRecord.class))).thenAnswer(invocation -> {
            LoanRecord lr = invocation.getArgument(0);
            lr.setId(10L);
            return lr;
        });

        LoanRecordResponse response = loanRecordService.createLoanRecord(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(LoanType.LENT, response.getType());
        assertEquals("John Doe", response.getPersonName());
        assertEquals(BigDecimal.valueOf(100.00), response.getOriginalAmount());
        assertEquals(BigDecimal.valueOf(100.00), response.getRemainingAmount());
        assertEquals(LoanStatus.ACTIVE, response.getStatus());
        verify(loanRecordRepository, times(1)).save(any(LoanRecord.class));
    }

    @Test
    void getLoanRecordById_Success() {
        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));

        LoanRecordResponse response = loanRecordService.getLoanRecordById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("John Doe", response.getPersonName());
    }

    @Test
    void getLoanRecordById_NotFound_ThrowsLoanRecordNotFoundException() {
        when(loanRecordRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(LoanRecordNotFoundException.class, () -> loanRecordService.getLoanRecordById(99L));
    }

    @Test
    void getLoanRecords_WithFilters_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<LoanRecord> page = new PageImpl<>(List.of(sampleRecord), pageable, 1);

        when(loanRecordRepository.findByUserIdAndTypeAndStatus(1L, LoanType.LENT, LoanStatus.ACTIVE, pageable))
                .thenReturn(page);

        Page<LoanRecordResponse> response = loanRecordService.getLoanRecords(LoanType.LENT, LoanStatus.ACTIVE, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("John Doe", response.getContent().get(0).getPersonName());
    }

    @Test
    void updateLoanRecord_Success() {
        UpdateLoanRecordRequest request = new UpdateLoanRecordRequest(
                LoanType.BORROWED,
                "Jane Doe",
                "Updated description",
                LocalDate.of(2026, 11, 30)
        );

        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenAnswer(i -> i.getArgument(0));

        LoanRecordResponse response = loanRecordService.updateLoanRecord(10L, request);

        assertNotNull(response);
        assertEquals(LoanType.BORROWED, response.getType());
        assertEquals("Jane Doe", response.getPersonName());
        assertEquals("Updated description", response.getDescription());
        // Amounts & Status must remain unchanged
        assertEquals(BigDecimal.valueOf(100.00), response.getOriginalAmount());
        assertEquals(BigDecimal.valueOf(100.00), response.getRemainingAmount());
        assertEquals(LoanStatus.ACTIVE, response.getStatus());
    }

    @Test
    void deleteLoanRecord_Success() {
        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));

        loanRecordService.deleteLoanRecord(10L);

        verify(loanRecordRepository, times(1)).delete(sampleRecord);
    }

    @Test
    void recordPayment_PartialPayment_Success() {
        LoanPaymentRequest payment = new LoanPaymentRequest(BigDecimal.valueOf(40.00));

        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenAnswer(i -> i.getArgument(0));

        LoanRecordResponse response = loanRecordService.recordPayment(10L, payment);

        assertNotNull(response);
        assertEquals(0, BigDecimal.valueOf(60.00).compareTo(response.getRemainingAmount()));
        assertEquals(LoanStatus.ACTIVE, response.getStatus());
    }

    @Test
    void recordPayment_FullPayment_AutoSettlement_Success() {
        LoanPaymentRequest payment = new LoanPaymentRequest(BigDecimal.valueOf(100.00));

        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenAnswer(i -> i.getArgument(0));

        LoanRecordResponse response = loanRecordService.recordPayment(10L, payment);

        assertNotNull(response);
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRemainingAmount()));
        assertEquals(LoanStatus.SETTLED, response.getStatus());
    }

    @Test
    void recordPayment_AlreadySettled_ThrowsException() {
        sampleRecord.setStatus(LoanStatus.SETTLED);
        sampleRecord.setRemainingAmount(BigDecimal.ZERO);

        LoanPaymentRequest payment = new LoanPaymentRequest(BigDecimal.valueOf(10.00));
        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));

        assertThrows(InvalidLoanPaymentException.class, () -> loanRecordService.recordPayment(10L, payment));
    }

    @Test
    void recordPayment_ExceedsRemainingAmount_ThrowsException() {
        LoanPaymentRequest payment = new LoanPaymentRequest(BigDecimal.valueOf(150.00));
        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));

        assertThrows(InvalidLoanPaymentException.class, () -> loanRecordService.recordPayment(10L, payment));
    }

    @Test
    void updateLoanRecord_UpdateOriginalAmount_Success() {
        // sampleRecord has original 100, remaining 100, repaid 0
        UpdateLoanRecordRequest request = new UpdateLoanRecordRequest(
                LoanType.LENT,
                "John Doe",
                "Updated loan",
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(150.00)
        );

        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenAnswer(i -> i.getArgument(0));

        LoanRecordResponse response = loanRecordService.updateLoanRecord(10L, request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(150.00), response.getOriginalAmount());
        assertEquals(BigDecimal.valueOf(150.00), response.getRemainingAmount());
        assertEquals(BigDecimal.valueOf(150.00), response.getTotalPrincipal());
        verify(loanEventRepository, times(1)).save(any(LoanEvent.class));
    }

    @Test
    void updateLoanRecord_UpdateOriginalAmount_LessThanAlreadyRepaid_ThrowsException() {
        // Suppose 60 already repaid: remaining is 40, original is 100
        sampleRecord.setRemainingAmount(BigDecimal.valueOf(40.00));

        UpdateLoanRecordRequest request = new UpdateLoanRecordRequest(
                LoanType.LENT,
                "John Doe",
                "Updated loan",
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(50.00) // 50 < 60 repaid
        );

        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));

        assertThrows(InvalidLoanPaymentException.class, () -> loanRecordService.updateLoanRecord(10L, request));
    }

    @Test
    void addMoreMoney_ActiveLoan_Success() {
        AddLoanPrincipalRequest request = new AddLoanPrincipalRequest(
                BigDecimal.valueOf(50.00),
                LocalDate.of(2026, 10, 25),
                "Additional funds for lunch"
        );

        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenAnswer(i -> i.getArgument(0));

        LoanRecordResponse response = loanRecordService.addMoreMoney(10L, request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(100.00), response.getOriginalAmount());
        assertEquals(BigDecimal.valueOf(50.00), response.getAdditionalAmount());
        assertEquals(BigDecimal.valueOf(150.00), response.getTotalPrincipal());
        assertEquals(BigDecimal.valueOf(150.00), response.getRemainingAmount());
        assertEquals(LoanStatus.ACTIVE, response.getStatus());
        verify(loanEventRepository, times(1)).save(any(LoanEvent.class));
    }

    @Test
    void addMoreMoney_SettledLoan_ReactivatesAndUpdatesBalances() {
        sampleRecord.setStatus(LoanStatus.SETTLED);
        sampleRecord.setRemainingAmount(BigDecimal.ZERO);

        AddLoanPrincipalRequest request = new AddLoanPrincipalRequest(
                BigDecimal.valueOf(75.00),
                LocalDate.of(2026, 10, 25),
                "Reopening loan with extra money"
        );

        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenAnswer(i -> i.getArgument(0));

        LoanRecordResponse response = loanRecordService.addMoreMoney(10L, request);

        assertNotNull(response);
        assertEquals(LoanStatus.ACTIVE, response.getStatus());
        assertEquals(BigDecimal.valueOf(75.00), response.getRemainingAmount());
        assertEquals(BigDecimal.valueOf(75.00), response.getAdditionalAmount());
        assertEquals(BigDecimal.valueOf(175.00), response.getTotalPrincipal());
    }

    @Test
    void addMoreMoney_ZeroOrNegativeAmount_ThrowsException() {
        AddLoanPrincipalRequest request = new AddLoanPrincipalRequest(
                BigDecimal.ZERO,
                LocalDate.of(2026, 10, 25),
                "Invalid zero amount"
        );

        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));

        assertThrows(InvalidLoanPaymentException.class, () -> loanRecordService.addMoreMoney(10L, request));
    }

    @Test
    void recordPayment_WithNotesAndDate_Success() {
        LoanPaymentRequest payment = new LoanPaymentRequest(
                BigDecimal.valueOf(30.00),
                LocalDate.of(2026, 10, 25),
                "Part payment via UPI"
        );

        when(loanRecordRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleRecord));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenAnswer(i -> i.getArgument(0));

        LoanRecordResponse response = loanRecordService.recordPayment(10L, payment);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(70.00), response.getRemainingAmount());
        assertEquals(LoanStatus.ACTIVE, response.getStatus());
        verify(loanEventRepository, times(1)).save(any(LoanEvent.class));
    }
}
