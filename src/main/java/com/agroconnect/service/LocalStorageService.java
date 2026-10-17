package com.agroconnect.service;

import com.agroconnect.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final Path root;

    public LocalStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("Please choose a photo");
        if (file.getSize() > MAX_BYTES) throw bad("Photo is larger than 5 MB");
        String ext;
        try (InputStream in = file.getInputStream()) {
            ext = detectType(in.readNBytes(12));     // we check the real file content, not just the file name
        } catch (IOException e) {
            throw bad("Could not read the photo");
        }
        if (ext == null) throw bad("Only JPG, PNG or WEBP photos are allowed");
        String name = UUID.randomUUID() + "." + ext;  // our own name: user file names are never trusted
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "Could not save the photo");
        }
        return "/uploads/" + name;
    }

    @Override
    public void delete(String urlPath) {
        if (urlPath == null || !urlPath.startsWith("/uploads/")) return;
        try {
            Path target = root.resolve(urlPath.substring("/uploads/".length())).normalize();
            if (target.startsWith(root)) Files.deleteIfExists(target);
        } catch (IOException ignored) { }
    }

    private static String detectType(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "jpg";
        if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "png";
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return "webp";
        return null;
    }

    private static ApiException bad(String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", msg);
    }
}
