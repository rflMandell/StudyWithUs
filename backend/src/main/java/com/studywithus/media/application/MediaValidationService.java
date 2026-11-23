package com.studywithus.media.application;

import com.studywithus.shared.domain.DomainException;
import org.springframework.http.HttpStatus;

import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MediaValidationService {

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final Map<String, Set<String>> MIME_TO_EXTENSIONS = Map.of(
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png"),
            "image/webp", Set.of("webp")
    );

    private MediaValidationService() {
    }

    public static String extractAndValidateExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_FILENAME", "Filename must not be blank");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.indexOf('\0') >= 0) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_FILENAME", "Filename contains forbidden path characters");
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_FILE_EXTENSION", "File must have a valid image extension (.jpg, .jpeg, .png, .webp)");
        }
        String ext = filename.substring(lastDot + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_EXTENSION", "Unsupported extension: " + ext);
        }
        return ext;
    }

    public static void validateDeclaredContentType(String contentType, String extension) {
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CONTENT_TYPE", "Unsupported content type: " + contentType);
        }
        Set<String> validExts = MIME_TO_EXTENSIONS.get(contentType.toLowerCase());
        if (validExts == null || !validExts.contains(extension.toLowerCase())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "CONTENT_TYPE_EXTENSION_MISMATCH", "Content-Type does not match file extension");
        }
    }

    public static void validateSize(long sizeBytes, long maxFileSizeBytes) {
        if (sizeBytes <= 0) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_FILE_SIZE", "File size must be greater than 0");
        }
        if (sizeBytes > maxFileSizeBytes) {
            throw new DomainException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "File size exceeds maximum allowed limit of " + maxFileSizeBytes + " bytes");
        }
    }

    /**
     * Detects the actual MIME type by inspecting file magic bytes (never trusting client headers).
     */
    public static Optional<String> detectRealMimeType(byte[] data) {
        if (data == null || data.length < 12) {
            return Optional.empty();
        }
        if (containsActiveScriptOrSvgSignature(data)) {
            return Optional.empty();
        }
        // JPEG: FF D8 FF
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47
                && data[4] == 0x0D && data[5] == 0x0A && data[6] == 0x1A && data[7] == 0x0A) {
            return Optional.of("image/png");
        }
        // WebP: "RIFF" .... "WEBP"
        if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    public static boolean matchesExtension(String detectedMimeType, String filename) {
        String ext = extractAndValidateExtension(filename);
        Set<String> validExts = MIME_TO_EXTENSIONS.get(detectedMimeType);
        return validExts != null && validExts.contains(ext);
    }

    public static void validateSafeStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_STORAGE_KEY", "Storage key must not be blank");
        }
        String trimmed = storageKey.trim();
        if (trimmed.contains("..") || trimmed.contains("://") || trimmed.startsWith("/")
                || trimmed.contains("\\") || trimmed.indexOf('\0') >= 0) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_STORAGE_KEY", "Storage key contains forbidden characters");
        }
    }

    public static void validateSafeExternalUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_EXTERNAL_URL", "URL must not be blank");
        }
        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "UNSAFE_URL_SCHEME", "Only HTTP/HTTPS URLs are allowed");
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_EXTERNAL_URL", "URL host is missing");
            }
            String lowerHost = host.toLowerCase(Locale.ROOT);
            if ("localhost".equals(lowerHost) || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "SSRF_BLOCKED", "Internal or local hosts are not allowed");
            }
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "SSRF_BLOCKED", "Private or loopback IP addresses are not allowed");
            }
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_EXTERNAL_URL", "Invalid external URL");
        }
    }

    private static boolean containsActiveScriptOrSvgSignature(byte[] data) {
        int inspectLen = Math.min(data.length, 512);
        String header = new String(data, 0, inspectLen, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return header.contains("<svg")
                || header.contains("<?xml")
                || header.contains("<html")
                || header.contains("<script")
                || header.contains("javascript:");
    }
}
