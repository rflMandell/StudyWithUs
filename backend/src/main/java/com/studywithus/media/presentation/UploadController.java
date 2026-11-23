package com.studywithus.media.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.media.application.ConfirmUploadCommand;
import com.studywithus.media.application.ConfirmUploadUseCase;
import com.studywithus.media.application.GetMediaAssetUseCase;
import com.studywithus.media.application.MediaAssetResult;
import com.studywithus.media.application.RequestUploadUrlUseCase;
import com.studywithus.media.application.UploadUrlResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final RequestUploadUrlUseCase requestUploadUrlUseCase;
    private final ConfirmUploadUseCase confirmUploadUseCase;
    private final GetMediaAssetUseCase getMediaAssetUseCase;

    @PostMapping
    public ResponseEntity<UploadUrlResponse> requestUploadUrl(
            @Valid @RequestBody RequestUploadUrlRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UploadUrlResult result = requestUploadUrlUseCase.execute(request.toCommand(principal.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UploadUrlResponse.from(result));
    }

    @PostMapping("/{mediaId}/confirm")
    public ResponseEntity<MediaAssetResponse> confirmUpload(
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        MediaAssetResult result = confirmUploadUseCase.execute(new ConfirmUploadCommand(mediaId, principal.getId()));
        return ResponseEntity.ok(MediaAssetResponse.from(result));
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaAssetResponse> getMediaAsset(@PathVariable UUID mediaId) {
        MediaAssetResult result = getMediaAssetUseCase.execute(mediaId);
        return ResponseEntity.ok(MediaAssetResponse.from(result));
    }
}
