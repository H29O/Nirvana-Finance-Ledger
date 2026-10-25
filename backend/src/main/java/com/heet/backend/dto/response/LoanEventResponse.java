package com.heet.backend.dto.response;

import com.heet.backend.enums.LoanEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEventResponse {

    private Long id;
    private LoanEventType eventType;
    private BigDecimal amount;
    private String notes;
    private LocalDateTime eventDate;
    private LocalDateTime createdAt;
}
