package aptms.services.impl;

import aptms.enums.UploadCategory;
import aptms.services.FileStorageService;
import aptms.services.UploadValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final UploadValidationService uploadValidationService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public String storeImage(MultipartFile file, String subDirectory) {
        String extension = uploadValidationService.validate(file, UploadCategory.IMAGE);

        // Never trust the client-supplied filename for the path — generate our own.
        String safeSubDir = subDirectory.replaceAll("[^a-zA-Z0-9_-]", "");
        String filename = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = Paths.get(uploadDir, safeSubDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(filename).normalize();
            if (!targetFile.getParent().equals(targetDir)) {
                // Should be unreachable given the generated filename, but guard anyway.
                throw new IllegalStateException("Invalid file path");
            }

            file.transferTo(targetFile);
            log.info("Stored uploaded image at {}", targetFile);

            return "/uploads/" + safeSubDir + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to store uploaded image", e);
            throw new IllegalStateException("Failed to store uploaded image");
        }
    }
}
