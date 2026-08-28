package com.heet.backend.service;

import com.heet.backend.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileSystemReceiptStorageService implements ReceiptStorageService {

    private final Path baseStorageLocation;

    public FileSystemReceiptStorageService(@Value("${app.receipt.storage-path:uploads/receipts}") String baseStoragePath) {
        this.baseStorageLocation = Paths.get(baseStoragePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create base directory for receipt uploads.", ex);
        }
    }

    @Override
    public String storeFile(Long userId, MultipartFile file, String storedFileName) {
        try {
            if (file.isEmpty()) {
                throw new FileStorageException("Cannot store empty file.");
            }

            Path userDir = this.baseStorageLocation.resolve(userId.toString()).normalize();
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
            }

            Path targetLocation = userDir.resolve(storedFileName).normalize();
            if (!targetLocation.startsWith(this.baseStorageLocation)) {
                throw new FileStorageException("Cannot store file outside current storage directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return targetLocation.toString();
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + storedFileName + ". Please try again.", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).normalize();
            if (!filePath.startsWith(this.baseStorageLocation)) {
                throw new FileStorageException("Cannot access file outside current storage directory.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("File not found or not readable at " + storagePath);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found at " + storagePath, ex);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).normalize();
            if (filePath.startsWith(this.baseStorageLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file at " + storagePath, ex);
        }
    }
}
