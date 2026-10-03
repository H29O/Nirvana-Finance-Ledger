package com.heet.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ReceiptStorageService {

    String storeFile(Long userId, MultipartFile file, String storedFileName);

    Resource loadFileAsResource(String storagePath);

    void deleteFile(String storagePath);

    void deleteUserStorage(Long userId);
}
