package aptms.services.impl;

import aptms.enums.UploadCategory;
import aptms.services.UploadValidationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class UploadValidationServiceImpl implements UploadValidationService {

    private static final long IMAGE_MAX_BYTES = 5L * 1024 * 1024;
    private static final long DOCUMENT_MAX_BYTES = 10L * 1024 * 1024;

    /** Detected (real, magic-byte-sniffed) content type -> canonical stored extension. */
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private static final Map<String, String> ALLOWED_DOCUMENT_TYPES = Map.of(
            "application/pdf", "pdf"
    );

    /** Declared filename extensions accepted per detected type, as a defense-in-depth
     *  cross-check against a client naming a file misleadingly (e.g. "invoice.exe.jpg"
     *  masking a real image behind a suspicious double extension is still fine content-wise,
     *  but a mismatched declared extension for the detected type is rejected). */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of("pdf");

    private final Tika tika = new Tika();

    @Override
    public String validate(MultipartFile file, UploadCategory category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        long maxBytes = category == UploadCategory.IMAGE ? IMAGE_MAX_BYTES : DOCUMENT_MAX_BYTES;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "File exceeds the " + (maxBytes / (1024 * 1024)) + "MB limit for "
                            + category.name().toLowerCase() + " uploads");
        }

        Map<String, String> allowedTypes = category == UploadCategory.IMAGE ? ALLOWED_IMAGE_TYPES : ALLOWED_DOCUMENT_TYPES;
        Set<String> allowedExtensions = category == UploadCategory.IMAGE ? ALLOWED_IMAGE_EXTENSIONS : ALLOWED_DOCUMENT_EXTENSIONS;
        String allowedLabel = category == UploadCategory.IMAGE ? "JPEG, PNG, WEBP" : "PDF";

        String detectedType = detectContentType(file);
        String extension = allowedTypes.get(detectedType);
        if (extension == null) {
            log.warn("Rejected upload: detected content type {} not allowed for category {}", detectedType, category);
            throw new IllegalArgumentException("Unsupported file type. Allowed: " + allowedLabel);
        }

        String declaredExt = extractExtension(file.getOriginalFilename());
        if (declaredExt != null && !allowedExtensions.contains(declaredExt.toLowerCase())) {
            log.warn("Rejected upload: declared extension '{}' doesn't match detected type {}", declaredExt, detectedType);
            throw new IllegalArgumentException("Unsupported file type. Allowed: " + allowedLabel);
        }

        return extension;
    }

    private String detectContentType(MultipartFile file) {
        try {
            // Tika.detect(byte[]) sniffs magic bytes/structure — the client-supplied
            // Content-Type header and filename are never consulted here.
            return tika.detect(file.getBytes());
        } catch (IOException e) {
            log.error("Failed to read uploaded file for content detection", e);
            throw new IllegalArgumentException("Could not read the uploaded file");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return null;
        return filename.substring(dot + 1);
    }
}
