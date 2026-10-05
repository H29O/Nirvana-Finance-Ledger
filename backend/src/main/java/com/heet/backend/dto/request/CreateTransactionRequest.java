package com.heet.backend.dto.request;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.heet.backend.enums.EntrySource;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Entry source is required")
    private EntrySource entrySource;

    @NotBlank(message = "Merchant cannot be blank")
    private String merchant;
    private String notes;
    private Long categoryId;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss]]")
    private java.time.LocalDateTime transactionDate;

    public CreateTransactionRequest(
            BigDecimal amount,
            TransactionType transactionType,
            PaymentMethod paymentMethod,
            EntrySource entrySource,
            String merchant,
            String notes,
            Long categoryId) {
        this(amount, transactionType, paymentMethod, entrySource, merchant, notes, categoryId, null);
    }
}
