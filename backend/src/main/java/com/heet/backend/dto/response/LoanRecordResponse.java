package com.heet.backend.dto.response;

import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
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
public class LoanRecordResponse {

    private Long id;
    private LoanType type;
    private String personName;
    private String description;
    private BigDecimal originalAmount;
    private BigDecimal remainingAmount;
    private LoanStatus status;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
