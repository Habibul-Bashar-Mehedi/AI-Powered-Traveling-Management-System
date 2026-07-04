package aptms.services.impl;

import aptms.exceptions.InvalidException;
import aptms.services.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public String storeImage(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new InvalidException("Image file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidException("Image must be 5MB or smaller");
        }

        String contentType = file.getContentType();
        String extension = ALLOWED_IMAGE_TYPES.get(contentType);
        if (extension == null) {
            throw new InvalidException("Unsupported image type. Allowed: JPEG, PNG, WEBP, GIF");
        }
        // Defense in depth: also check the declared extension against an allowlist,
        // in case a client spoofs the content-type header.
        String declaredExt = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (declaredExt != null && !ALLOWED_EXTENSIONS.contains(declaredExt.toLowerCase())) {
            throw new InvalidException("Unsupported image type. Allowed: JPEG, PNG, WEBP, GIF");
        }

        // Never trust the client-supplied filename for the path — generate our own.
        String safeSubDir = subDirectory.replaceAll("[^a-zA-Z0-9_-]", "");
        String filename = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = Paths.get(uploadDir, safeSubDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(filename).normalize();
            if (!targetFile.getParent().equals(targetDir)) {
                // Should be unreachable given the generated filename, but guard anyway.
                throw new InvalidException("Invalid file path");
            }

            file.transferTo(targetFile);
            log.info("Stored uploaded image at {}", targetFile);

            return "/uploads/" + safeSubDir + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to store uploaded image", e);
            throw new InvalidException("Failed to store uploaded image");
        }
    }
}
