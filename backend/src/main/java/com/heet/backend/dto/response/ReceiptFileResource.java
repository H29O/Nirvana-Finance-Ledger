package com.heet.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
@AllArgsConstructor
public class ReceiptFileResource {

    private final Resource resource;
    private final String contentType;
    private final String originalFileName;
}
