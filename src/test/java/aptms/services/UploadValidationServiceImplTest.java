package aptms.services;

import aptms.enums.UploadCategory;
import aptms.services.impl.UploadValidationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadValidationServiceImplTest {

    private final UploadValidationServiceImpl service = new UploadValidationServiceImpl();

    // Minimal real magic-byte signatures — enough for Tika to detect the true type.
    private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] PDF_BYTES = "%PDF-1.4\n".getBytes();
    private static final byte[] HTML_BYTES = "<html><body>not an image</body></html>".getBytes();

    @Test
    void acceptsRealPngRegardlessOfDeclaredContentTypeHeader() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);
        String ext = service.validate(file, UploadCategory.IMAGE);
        assertEquals("png", ext);
    }

    @Test
    void acceptsRealJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);
        String ext = service.validate(file, UploadCategory.IMAGE);
        assertEquals("jpg", ext);
    }

    @Test
    void rejectsFileRenamedToLookLikeAnImageButActuallyHtml() {
        // Spoofed Content-Type + spoofed .png extension — content is what matters.
        MockMultipartFile file = new MockMultipartFile("file", "malicious.png", "image/png", HTML_BYTES);
        assertThrows(IllegalArgumentException.class, () -> service.validate(file, UploadCategory.IMAGE));
    }

    @Test
    void rejectsDeclaredExtensionMismatchedWithDetectedType() {
        // Real PNG bytes, but named/declared as a document — extension cross-check should reject it.
        MockMultipartFile file = new MockMultipartFile("file", "photo.pdf", "image/png", PNG_BYTES);
        assertThrows(IllegalArgumentException.class, () -> service.validate(file, UploadCategory.IMAGE));
    }

    @Test
    void acceptsRealPdfForDocumentCategory() {
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", PDF_BYTES);
        String ext = service.validate(file, UploadCategory.DOCUMENT);
        assertEquals("pdf", ext);
    }

    @Test
    void rejectsPngForDocumentCategory() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);
        assertThrows(IllegalArgumentException.class, () -> service.validate(file, UploadCategory.DOCUMENT));
    }

    @Test
    void rejectsOversizedImage() {
        byte[] oversized = new byte[6 * 1024 * 1024];
        System.arraycopy(PNG_BYTES, 0, oversized, 0, PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);
        assertThrows(IllegalArgumentException.class, () -> service.validate(file, UploadCategory.IMAGE));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> service.validate(file, UploadCategory.IMAGE));
    }
}
