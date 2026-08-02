package com.heet.backend.dto.request;

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

    private BigDecimal amount;
    private TransactionType transactionType;
    private PaymentMethod paymentMethod;
    private EntrySource entrySource;
    private String merchant;
    private String notes;
}
