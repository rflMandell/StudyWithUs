package com.studywithus.media.infrastructure;

import com.studywithus.media.application.MediaValidationService;
import com.studywithus.media.domain.ImageProcessorPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ImageIOProcessorAdapterTest {

    private final ImageIOProcessorAdapter adapter = new ImageIOProcessorAdapter();

    @Test
    @DisplayName("Converts PNG image to WebP and generates WebP thumbnail")
    void convertsPngToWebp() throws Exception {
        byte[] pngBytes = createImageBytes("png", 800, 600, BufferedImage.TYPE_INT_ARGB);

        ImageProcessorPort.ProcessedImage result = adapter.process(pngBytes, "image/png");

        assertThat(result.mimeType()).isEqualTo("image/webp");
        assertThat(result.width()).isEqualTo(800);
        assertThat(result.height()).isEqualTo(600);
        assertThat(MediaValidationService.detectRealMimeType(result.sanitizedBytes())).contains("image/webp");
        assertThat(MediaValidationService.detectRealMimeType(result.thumbnailBytes())).contains("image/webp");
        assertThat(result.thumbnailBytes().length).isLessThan(result.sanitizedBytes().length);
    }

    @Test
    @DisplayName("Converts JPEG image to WebP and strips metadata")
    void convertsJpegToWebp() throws Exception {
        byte[] jpegBytes = createImageBytes("jpeg", 500, 500, BufferedImage.TYPE_INT_RGB);

        ImageProcessorPort.ProcessedImage result = adapter.process(jpegBytes, "image/jpeg");

        assertThat(result.mimeType()).isEqualTo("image/webp");
        assertThat(result.width()).isEqualTo(500);
        assertThat(result.height()).isEqualTo(500);
        assertThat(MediaValidationService.detectRealMimeType(result.sanitizedBytes())).contains("image/webp");
        assertThat(MediaValidationService.detectRealMimeType(result.thumbnailBytes())).contains("image/webp");
    }

    @Test
    @DisplayName("Parses dimensions when input is already WebP")
    void preservesWebpAndParsesDimensions() throws Exception {
        byte[] pngBytes = createImageBytes("png", 320, 240, BufferedImage.TYPE_INT_ARGB);
        byte[] webpBytes = adapter.process(pngBytes, "image/png").sanitizedBytes();

        ImageProcessorPort.ProcessedImage result = adapter.process(webpBytes, "image/webp");

        assertThat(result.mimeType()).isEqualTo("image/webp");
        assertThat(result.width()).isEqualTo(320);
        assertThat(result.height()).isEqualTo(240);
        assertThat(MediaValidationService.detectRealMimeType(result.sanitizedBytes())).contains("image/webp");
    }

    private byte[] createImageBytes(String format, int width, int height, int imageType) throws Exception {
        BufferedImage img = new BufferedImage(width, height, imageType);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(30, 144, 255));
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }
}
