package com.heet.backend.dto.request;

import com.heet.backend.enums.LoanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanRecordRequest {

    @NotNull(message = "Loan type is required")
    private LoanType type;

    @NotBlank(message = "Person name is required")
    private String personName;

    private String description;

    @NotNull(message = "Original amount is required")
    @DecimalMin(value = "0.01", message = "Original amount must be greater than zero")
    private BigDecimal originalAmount;

    private LocalDate dueDate;
}
