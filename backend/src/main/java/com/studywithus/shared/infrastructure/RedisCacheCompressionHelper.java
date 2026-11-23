package com.studywithus.shared.infrastructure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class RedisCacheCompressionHelper {

    private static final String GZIP_PREFIX = "gz:";

    private RedisCacheCompressionHelper() {}

    public static String compressIfBeneficial(String raw) {
        if (raw == null || raw.length() < 256) {
            return raw;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(raw.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            String encoded = GZIP_PREFIX + Base64.getEncoder().encodeToString(baos.toByteArray());
            return encoded.length() < raw.length() ? encoded : raw;
        } catch (IOException e) {
            return raw;
        }
    }

    public static String decompressIfNeeded(String stored) {
        if (stored == null || !stored.startsWith(GZIP_PREFIX)) {
            return stored;
        }
        byte[] compressed = Base64.getDecoder().decode(stored.substring(GZIP_PREFIX.length()));
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decompress cached payload", e);
        }
    }
}
