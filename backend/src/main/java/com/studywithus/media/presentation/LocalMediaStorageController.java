package com.studywithus.media.presentation;

import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaAssetRepository;
import com.studywithus.media.domain.MediaStatus;
import com.studywithus.media.infrastructure.LocalObjectStorageAdapter;
import com.studywithus.shared.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/media")
@ConditionalOnProperty(name = "app.media.storage-type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalMediaStorageController {

    private final LocalObjectStorageAdapter storageAdapter;
    private final MediaAssetRepository mediaAssetRepository;

    @PutMapping("/storage/**")
    public ResponseEntity<Void> uploadToPresignedUrl(
            HttpServletRequest request,
            @RequestParam("expires") long expires,
            @RequestParam(value = "contentLength", required = false) Long contentLength,
            @RequestParam("signature") String signature,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestBody byte[] body
    ) {
        String storageKey = extractKey(request, "/api/v1/media/storage/");
        if (!storageAdapter.verifySignature(storageKey, contentType, contentLength, expires, signature)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "INVALID_UPLOAD_SIGNATURE", "Presigned URL is invalid or expired");
        }
        if (body.length > storageAdapter.getMaxFileSizeBytes()) {
            throw new DomainException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "File exceeds maximum allowed size");
        }
        if (body.length != contentLength) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_CONTENT_LENGTH", "Uploaded body size does not match signed content length");
        }
        MediaAsset asset = mediaAssetRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media asset not found"));
        if (asset.getStatus() != MediaStatus.PENDING) {
            throw new DomainException(HttpStatus.CONFLICT, "MEDIA_ALREADY_UPLOADED", "Media upload is no longer pending");
        }
        storageAdapter.upload(storageKey, body, contentType);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public/**")
    public ResponseEntity<byte[]> getPublicMedia(HttpServletRequest request) {
        String storageKey = extractKey(request, "/api/v1/media/public/");
        String lookupKey = storageKey.startsWith("thumbnails/")
                ? "uploads/" + storageKey.substring("thumbnails/".length())
                : storageKey;

        MediaAsset asset = mediaAssetRepository.findByStorageKey(lookupKey).orElse(null);
        if (asset != null && !asset.isVisible()) {
            throw new DomainException(HttpStatus.NOT_FOUND, "MEDIA_UNAVAILABLE", "Media is no longer available");
        }

        if (!storageAdapter.exists(storageKey)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media object not found");
        }

        byte[] data = storageAdapter.download(storageKey);
        String mimeType = asset != null && asset.getDetectedMimeType() != null
                ? asset.getDetectedMimeType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .body(data);
    }

    private String extractKey(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        int idx = uri.indexOf(prefix);
        if (idx < 0) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_STORAGE_KEY", "Invalid media path");
        }
        return uri.substring(idx + prefix.length());
    }
}
