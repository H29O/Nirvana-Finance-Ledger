package com.heet.backend.service.ocr;

import java.io.File;

public interface OcrService {

    OcrResult extractTextAndData(File file, String contentType);
}
