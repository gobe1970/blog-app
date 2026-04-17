package com.blog.blog_app.service;

import com.blog.blog_app.config.AppProperties;
import com.blog.blog_app.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final AppProperties appProperties;

    // Allowed MIME types — checked against actual file content, not just extension
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    // Allowed extensions — double-checked alongside MIME type
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    private Path uploadRootPath;    // Resolved once at startup via @PostConstruct

    // ------------------------------------------------------------------ //
    //  INITIALISATION                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Runs once after the bean is constructed and AppProperties is injected.
     * Creates the upload directory if it doesn't already exist.
     * Fails fast at startup rather than at the first upload request.
     */
    @PostConstruct
    public void init() {
        uploadRootPath = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRootPath);
            log.info("File storage initialised at: {}", uploadRootPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadRootPath, e);
        }
    }

    // ------------------------------------------------------------------ //
    //  PUBLIC API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Validates and stores an uploaded image file.
     *
     * @param file the incoming MultipartFile
     * @return the relative path string persisted in the DB  (e.g. "uploads/dev/uuid_photo.jpg")
     * @throws BadRequestException if the file fails any validation check
     */
    public String store(MultipartFile file) {
        validateFile(file);

        String safeFilename  = buildSafeFilename(file.getOriginalFilename());
        Path   targetPath    = uploadRootPath.resolve(safeFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file '{}' ({} bytes) at '{}'",
                    safeFilename, file.getSize(), targetPath);
        } catch (IOException e) {
            log.error("Failed to store file '{}': {}", safeFilename, e.getMessage(), e);
            throw new RuntimeException("Could not store file. Please try again.", e);
        }

        // Return a portable relative path — keeps the DB value environment-agnostic
        return appProperties.getUploadDir() + "/" + safeFilename;
    }

    /**
     * Deletes a previously stored file by its relative path.
     * Silently ignores missing files (idempotent).
     */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;

        try {
            Path target = uploadRootPath.resolve(
                    Paths.get(relativePath).getFileName()   // strip any directory prefix
            );
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                log.info("Deleted file '{}'", target);
            } else {
                log.warn("File not found for deletion: '{}'", target);
            }
        } catch (IOException e) {
            log.error("Could not delete file '{}': {}", relativePath, e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ //
    //  VALIDATION                                                          //
    // ------------------------------------------------------------------ //

    private void validateFile(MultipartFile file) {

        // 1. Null / empty check
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a file to upload.");
        }

        // 2. File size check (against our custom property, not just Spring's multipart limit)
        long maxBytes = (long) appProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(String.format(
                    "File size %.2f MB exceeds the maximum allowed size of %d MB.",
                    file.getSize() / (1024.0 * 1024.0),
                    appProperties.getMaxFileSizeMb()
            ));
        }

        // 3. Extension check
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException(String.format(
                    "File type '.%s' is not allowed. Allowed types: %s",
                    extension, ALLOWED_EXTENSIONS
            ));
        }

        // 4. MIME type check — more reliable than extension alone
        //    (prevents renaming 'malware.exe' to 'photo.jpg')
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(String.format(
                    "File MIME type '%s' is not allowed. Allowed types: %s",
                    contentType, ALLOWED_MIME_TYPES
            ));
        }

        // 5. Original filename sanity check — must not be blank or path-traversal attempt
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BadRequestException("File must have a valid name.");
        }
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BadRequestException("Filename contains invalid characters.");
        }

        log.debug("File validation passed — name='{}', size={} bytes, type='{}'",
                originalFilename, file.getSize(), contentType);
    }

    // ------------------------------------------------------------------ //
    //  PRIVATE HELPERS                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Produces a collision-safe filename: {@code <UUID>_<cleanedOriginalName>}
     * e.g. "a1b2c3d4-..._my-photo.jpg"
     */
    private String buildSafeFilename(String originalFilename) {
        // StringUtils.cleanPath neutralises path traversal sequences like ../
        String cleaned = StringUtils.cleanPath(
                originalFilename != null ? originalFilename : "upload"
        );
        return UUID.randomUUID() + "_" + cleaned;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestException("File must have a valid extension.");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

}