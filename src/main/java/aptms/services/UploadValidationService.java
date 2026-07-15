package aptms.services;

import aptms.enums.UploadCategory;
import org.springframework.web.multipart.MultipartFile;

/**
 * Shared, reusable validation for every file-upload endpoint in the app
 * (vendor service images, banner images, and any future document upload).
 *
 * Validates the file's actual content via magic-byte detection, not the
 * client-supplied filename extension or Content-Type header, since both
 * are trivially spoofable.
 */
public interface UploadValidationService {

    /**
     * Validates the given file against the rules for its category (allowed
     * content types, max size). Throws {@link IllegalArgumentException} with
     * a user-facing message if validation fails.
     *
     * @return the canonical file extension to use for the stored file (e.g. "jpg", "pdf"),
     *         based on the file's actual detected content — never the client-supplied name.
     */
    String validate(MultipartFile file, UploadCategory category);
}
