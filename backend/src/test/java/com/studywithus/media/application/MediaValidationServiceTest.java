package com.studywithus.media.application;

import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaValidationServiceTest {

    @Test
    @DisplayName("Detects JPEG, PNG, and WebP magic bytes accurately")
    void detectsValidMagicBytes() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        byte[] webp = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        byte[] fake = "<html><script>alert(1)</script></html>".getBytes();

        assertThat(MediaValidationService.detectRealMimeType(jpeg)).contains("image/jpeg");
        assertThat(MediaValidationService.detectRealMimeType(png)).contains("image/png");
        assertThat(MediaValidationService.detectRealMimeType(webp)).contains("image/webp");
        assertThat(MediaValidationService.detectRealMimeType(fake)).isEmpty();
    }

    @Test
    @DisplayName("Rejects unsupported extensions and content-type mismatches")
    void rejectsUnsupportedExtensionsAndMismatches() {
        assertThatThrownBy(() -> MediaValidationService.extractAndValidateExtension("malware.exe"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo("UNSUPPORTED_FILE_EXTENSION"));

        assertThatThrownBy(() -> MediaValidationService.validateDeclaredContentType("image/png", "jpg"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo("CONTENT_TYPE_EXTENSION_MISMATCH"));
    }

    @Test
    @DisplayName("Rejects files exceeding maximum size limit")
    void rejectsOversizedFile() {
        assertThatThrownBy(() -> MediaValidationService.validateSize(6_000_000L, 5_242_880L))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }
}
