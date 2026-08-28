package com.heet.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {

    private Long id;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private Long transactionId;
}
