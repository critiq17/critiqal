package org.critiqal.infra.security;

import org.critiqal.domain.shared.exception.DomainException;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class ImageValidator {

    private ImageValidator() {}

    public static void validate(FileUpload file) throws IOException {
        var allowed = java.util.Set.of("image/jpeg", "image/png", "image/webp", "image/heic");
        if (!allowed.contains(file.contentType())) {
            throw new DomainException("Only images are allowed");
        }
        if (file.size() > 10 * 1024 * 1024) {
            throw new DomainException("Max file size is 10MB");
        }
        validateMagicBytes(file);
    }

    private static void validateMagicBytes(FileUpload file) throws IOException {
        byte[] header = new byte[12];
        try (InputStream in = Files.newInputStream(file.uploadedFile())) {
            int read = in.readNBytes(header, 0, 12);
            if (read < 3) {
                throw new DomainException("Invalid image file");
            }
        }

        if (isJpeg(header) || isPng(header) || isWebp(header)) {
            return;
        }
        throw new DomainException("Invalid image file");
    }

    private static boolean isJpeg(byte[] h) {
        return (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] h) {
        return (h[0] & 0xFF) == 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47;
    }

    // RIFF....WEBP
    private static boolean isWebp(byte[] h) {
        return h[0] == 0x52 && h[1] == 0x49 && h[2] == 0x46 && h[3] == 0x46
                && h[8] == 0x57 && h[9] == 0x45 && h[10] == 0x42 && h[11] == 0x50;
    }
}
