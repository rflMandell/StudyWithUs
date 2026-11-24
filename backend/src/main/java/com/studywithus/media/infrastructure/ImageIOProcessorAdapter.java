package com.studywithus.media.infrastructure;

import com.studywithus.media.domain.ImageProcessorPort;
import com.studywithus.shared.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class ImageIOProcessorAdapter implements ImageProcessorPort {

    private static final int MAX_THUMBNAIL_DIMENSION = 400;
    private static final float WEBP_QUALITY = 0.85f;
    private static final String WEBP_MIME_TYPE = "image/webp";

    @Override
    public ProcessedImage process(byte[] rawBytes, String mimeType) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (source == null) {
                if (WEBP_MIME_TYPE.equals(mimeType)) {
                    int[] dims = parseWebpDimensions(rawBytes);
                    return new ProcessedImage(rawBytes, rawBytes, dims[0], dims[1], WEBP_MIME_TYPE);
                }
                throw new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "CORRUPT_IMAGE", "Unable to decode image content");
            }

            int width = source.getWidth();
            int height = source.getHeight();
            boolean preserveAlpha = source.getColorModel().hasAlpha();

            // Re-drawing into a fresh BufferedImage strips all EXIF, GPS, and embedded metadata
            BufferedImage cleanOriginal = copyWithoutMetadata(source, preserveAlpha);
            byte[] sanitizedBytes = encodeWebp(cleanOriginal);

            BufferedImage thumbnail = scaleProportionally(cleanOriginal, MAX_THUMBNAIL_DIMENSION, preserveAlpha);
            byte[] thumbnailBytes = encodeWebp(thumbnail);

            return new ProcessedImage(sanitizedBytes, thumbnailBytes, width, height, WEBP_MIME_TYPE);
        } catch (DomainException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_PROCESSING_ERROR", "Failed to process image");
        }
    }

    private BufferedImage copyWithoutMetadata(BufferedImage source, boolean preserveAlpha) {
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage clean = new BufferedImage(source.getWidth(), source.getHeight(), imageType);
        Graphics2D g2d = clean.createGraphics();
        try {
            g2d.drawImage(source, 0, 0, null);
        } finally {
            g2d.dispose();
        }
        return clean;
    }

    private BufferedImage scaleProportionally(BufferedImage source, int maxDimension, boolean preserveAlpha) {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();

        if (srcWidth <= maxDimension && srcHeight <= maxDimension) {
            return source;
        }

        double ratio = Math.min((double) maxDimension / srcWidth, (double) maxDimension / srcHeight);
        int targetWidth = Math.max(1, (int) Math.round(srcWidth * ratio));
        int targetHeight = Math.max(1, (int) Math.round(srcHeight * ratio));

        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }
        return resized;
    }

    private byte[] encodeWebp(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        if (writers.hasNext()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(WEBP_QUALITY);
                }
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
            if (baos.size() > 0) {
                return baos.toByteArray();
            }
        }
        return WebpEncoder.encode(image);
    }

    private int[] parseWebpDimensions(byte[] data) {
        if (data == null || data.length < 30) {
            return new int[]{0, 0};
        }
        // VP8L (lossless): chunk at offset 12, signature 0x2F at offset 20
        if (data[12] == 'V' && data[13] == 'P' && data[14] == '8' && data[15] == 'L' && data[20] == 0x2F) {
            int b1 = data[21] & 0xFF;
            int b2 = data[22] & 0xFF;
            int b3 = data[23] & 0xFF;
            int b4 = data[24] & 0xFF;
            int width = 1 + (((b2 & 0x3F) << 8) | b1);
            int height = 1 + (((b4 & 0x0F) << 10) | (b3 << 2) | ((b2 & 0xC0) >>> 6));
            return new int[]{width, height};
        }
        // VP8X (extended): chunk at offset 12, width at 24..26, height at 27..29 (24-bit LE + 1)
        if (data[12] == 'V' && data[13] == 'P' && data[14] == '8' && data[15] == 'X') {
            int width = 1 + ((data[24] & 0xFF) | ((data[25] & 0xFF) << 8) | ((data[26] & 0xFF) << 16));
            int height = 1 + ((data[27] & 0xFF) | ((data[28] & 0xFF) << 8) | ((data[29] & 0xFF) << 16));
            return new int[]{width, height};
        }
        // VP8 (lossy): keyframe start code 9D 01 2A at offset 23..25
        if (data[12] == 'V' && data[13] == 'P' && data[14] == '8' && data[15] == ' '
                && (data[23] & 0xFF) == 0x9D && (data[24] & 0xFF) == 0x01 && (data[25] & 0xFF) == 0x2A) {
            int width = ((data[26] & 0xFF) | ((data[27] & 0x3F) << 8));
            int height = ((data[28] & 0xFF) | ((data[29] & 0x3F) << 8));
            return new int[]{width, height};
        }
        return new int[]{0, 0};
    }
}
