package com.studywithus.media.infrastructure;

import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.shared.domain.DomainException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.media.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorageAdapter implements ObjectStoragePort {

    @Value("${app.media.local-dir:${java.io.tmpdir}/studywithus-media}")
    private String localDir;

    @Value("${app.media.public-base-url:http://localhost:8080/api/v1/media/public}")
    private String publicBaseUrl;

    @Value("${app.media.upload-base-url:http://localhost:8080/api/v1/media/storage}")
    private String uploadBaseUrl;

    @Value("${app.media.presign-secret}")
    private String presignSecret;

    @Value("${app.media.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    private Path rootPath;

    @PostConstruct
    public void init() throws IOException {
        rootPath = Paths.get(localDir).toAbsolutePath().normalize();
        Files.createDirectories(rootPath);
    }

    @Override
    public PresignedUploadResult generatePresignedUploadUrl(
            String storageKey,
            String contentType,
            long contentLength,
            Duration expiration
    ) {
        Instant expiresAt = Instant.now().plus(expiration);
        long expiresEpoch = expiresAt.getEpochSecond();
        String signature = sign(storageKey, contentType, contentLength, expiresEpoch);
        String url = String.format("%s/%s?expires=%d&contentLength=%d&signature=%s",
                stripTrailingSlash(uploadBaseUrl),
                storageKey,
                expiresEpoch,
                contentLength,
                signature);
        return new PresignedUploadResult(
                url,
                expiresAt,
                Map.of("Content-Type", contentType)
        );
    }

    @Override
    public boolean exists(String storageKey) {
        Path path = resolveSafePath(storageKey);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    @Override
    public StoredObjectMetadata getMetadata(String storageKey) {
        Path path = resolveSafePath(storageKey);
        if (!Files.exists(path)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "STORAGE_OBJECT_NOT_FOUND", "Object not found in storage");
        }
        try {
            long size = Files.size(path);
            String contentType = Files.probeContentType(path);
            return new StoredObjectMetadata(size, contentType);
        } catch (IOException ex) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_READ_ERROR", "Failed to read object metadata");
        }
    }

    @Override
    public byte[] download(String storageKey) {
        Path path = resolveSafePath(storageKey);
        if (!Files.exists(path)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "STORAGE_OBJECT_NOT_FOUND", "Object not found in storage");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_READ_ERROR", "Failed to download object");
        }
    }

    @Override
    public void upload(String storageKey, byte[] data, String contentType) {
        Path path = resolveSafePath(storageKey);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        } catch (IOException ex) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_WRITE_ERROR", "Failed to write object to storage");
        }
    }

    @Override
    public void delete(String storageKey) {
        Path path = resolveSafePath(storageKey);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_DELETE_ERROR", "Failed to delete object from storage");
        }
    }

    @Override
    public String getPublicUrl(String storageKey) {
        return stripTrailingSlash(publicBaseUrl) + "/" + storageKey;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public boolean verifySignature(String storageKey, String contentType, Long contentLength, long expiresEpoch, String signature) {
        if (signature == null || contentLength == null || contentLength <= 0 || contentLength > maxFileSizeBytes) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expiresEpoch) {
            return false;
        }
        String normalizedType = normalizeContentType(contentType);
        String expected = sign(storageKey, normalizedType, contentLength, expiresEpoch);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.trim().toLowerCase();
    }

    private String sign(String storageKey, String contentType, long contentLength, long expiresEpoch) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(presignSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            String payload = storageKey + ":" + normalizeContentType(contentType) + ":" + contentLength + ":" + expiresEpoch;
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign presigned upload URL", ex);
        }
    }

    private Path resolveSafePath(String storageKey) {
        if (storageKey == null || storageKey.contains("..")) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_STORAGE_KEY", "Invalid storage key");
        }
        Path resolved = rootPath.resolve(storageKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_STORAGE_KEY", "Storage key escapes root directory");
        }
        return resolved;
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
