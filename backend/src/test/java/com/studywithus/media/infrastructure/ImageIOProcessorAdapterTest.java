package com.studywithus.media.infrastructure;

import com.studywithus.media.domain.ImageProcessorPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ImageIOProcessorAdapterTest {

    private final ImageIOProcessorAdapter processor = new ImageIOProcessorAdapter();

    @Test
    @DisplayName("Sanitizes image and generates proportional thumbnail <= 400px")
    void sanitizesAndGeneratesThumbnail() throws Exception {
        BufferedImage large = new BufferedImage(1200, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = large.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 1200, 600);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(large, "png", baos);

        ImageProcessorPort.ProcessedImage result = processor.process(baos.toByteArray(), "image/png");

        assertThat(result.width()).isEqualTo(1200);
        assertThat(result.height()).isEqualTo(600);
        assertThat(result.sanitizedBytes()).isNotEmpty();
        assertThat(result.thumbnailBytes()).isNotEmpty();

        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(result.thumbnailBytes()));
        assertThat(thumb.getWidth()).isEqualTo(400);
        assertThat(thumb.getHeight()).isEqualTo(200);
    }
}
