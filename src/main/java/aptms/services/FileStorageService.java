package aptms.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Validates and stores an uploaded image under the given sub-directory.
     *
     * @return the public URL path the stored file can be served from (e.g. "/uploads/vendor-services/abc.jpg")
     */
    String storeImage(MultipartFile file, String subDirectory);
}
