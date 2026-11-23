package com.studywithus.media.domain;

public interface ImageProcessorPort {

    ProcessedImage process(byte[] rawBytes, String mimeType);

    record ProcessedImage(
            byte[] sanitizedBytes,
            byte[] thumbnailBytes,
            int width,
            int height,
            String mimeType
    ) {}
}
