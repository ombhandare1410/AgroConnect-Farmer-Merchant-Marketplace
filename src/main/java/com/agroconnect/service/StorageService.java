package com.agroconnect.service;

import org.springframework.web.multipart.MultipartFile;

/** Where fruit photos are kept. Today: local folder. Later: a cloud service, without changing callers. */
public interface StorageService {
    /** Validates and saves the file; returns the URL path to store in the database. */
    String store(MultipartFile file);
    void delete(String urlPath);
}
