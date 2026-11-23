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
    private static final float JPEG_QUALITY = 0.85f;

    @Override
    public ProcessedImage process(byte[] rawBytes, String mimeType) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (source == null) {
                if ("image/webp".equals(mimeType)) {
                    // Fallback when native WebP ImageIO plugin is unavailable: preserve bytes
                    return new ProcessedImage(rawBytes, rawBytes, 0, 0, mimeType);
                }
                throw new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "CORRUPT_IMAGE", "Unable to decode image content");
            }

            int width = source.getWidth();
            int height = source.getHeight();
            boolean isPng = "image/png".equals(mimeType);
            String outputFormat = isPng ? "png" : "jpeg";
            String outputMime = isPng ? "image/png" : "image/jpeg";

            // Re-drawing into a fresh BufferedImage strips all EXIF, GPS, and embedded metadata
            BufferedImage cleanOriginal = copyWithoutMetadata(source, isPng);
            byte[] sanitizedBytes = encode(cleanOriginal, outputFormat);

            BufferedImage thumbnail = scaleProportionally(cleanOriginal, MAX_THUMBNAIL_DIMENSION, isPng);
            byte[] thumbnailBytes = encode(thumbnail, outputFormat);

            return new ProcessedImage(sanitizedBytes, thumbnailBytes, width, height, outputMime);
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

    private byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if ("jpeg".equals(format)) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(JPEG_QUALITY);
                    }
                    writer.write(null, new IIOImage(image, null, null), param);
                } finally {
                    writer.dispose();
                }
                return baos.toByteArray();
            }
        }
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}
