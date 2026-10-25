package com.heet.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.heet.backend.enums.EntrySource;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss]]")
    private LocalDateTime transactionDate;

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

    @JsonSetter("transactionDate")
    public void setTransactionDateFromJson(Object value) {
        if (value == null) {
            this.transactionDate = null;
        } else if (value instanceof LocalDateTime ldt) {
            this.transactionDate = ldt;
        } else if (value instanceof String str) {
            str = str.trim();
            if (str.isEmpty()) {
                this.transactionDate = null;
            } else if (str.length() == 10 && str.matches("\\d{4}-\\d{2}-\\d{2}")) {
                this.transactionDate = LocalDate.parse(str).atStartOfDay();
            } else {
                try {
                    this.transactionDate = LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    try {
                        this.transactionDate = LocalDateTime.parse(str);
                    } catch (Exception ex) {
                        this.transactionDate = LocalDate.parse(str.substring(0, 10)).atStartOfDay();
                    }
                }
            }
        }
    }
}
