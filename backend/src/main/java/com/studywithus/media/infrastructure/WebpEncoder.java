package com.studywithus.media.infrastructure;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Pure-Java WebP (VP8L lossless) encoder compliant with RFC 9649.
 * Guarantees WebP output on any JVM/OS (including Alpine Linux containers)
 * without requiring native libwebp binaries.
 */
final class WebpEncoder {

    private static final byte VP8L_SIGNATURE = 0x2F;

    private WebpEncoder() {
    }

    static byte[] encode(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();

        BitWriter bits = new BitWriter(width * height * 4 + 256);

        // VP8L signature
        bits.writeBits(VP8L_SIGNATURE, 8);

        // Header: width-1 (14b), height-1 (14b), alpha_is_used (1b), version (3b = 0)
        bits.writeBits(width - 1, 14);
        bits.writeBits(height - 1, 14);
        bits.writeBits(hasAlpha ? 1 : 0, 1);
        bits.writeBits(0, 3);

        // No transforms, no color cache, no meta-Huffman
        bits.writeBits(0, 1);
        bits.writeBits(0, 1);
        bits.writeBits(0, 1);

        // 5 prefix codes: Green+Length (280), Red (256), Blue (256), Alpha (256), Distance (40)
        writeFixed8BitPrefixCode(bits, true);  // Green + Length (max_symbol = 256)
        writeFixed8BitPrefixCode(bits, false); // Red
        writeFixed8BitPrefixCode(bits, false); // Blue
        writeFixed8BitPrefixCode(bits, false); // Alpha
        writeDummyDistanceCode(bits);          // Distance (unused for literal-only stream)

        // Pixel data in ARGB -> VP8L order (G, R, B, A)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int a = hasAlpha ? ((argb >>> 24) & 0xFF) : 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;

                bits.writeBits(reverse8(g), 8);
                bits.writeBits(reverse8(r), 8);
                bits.writeBits(reverse8(b), 8);
                bits.writeBits(reverse8(a), 8);
            }
        }

        byte[] vp8lPayload = bits.toByteArray();
        int paddedPayloadLen = vp8lPayload.length + (vp8lPayload.length & 1);
        int riffChunkSize = 4 + 8 + paddedPayloadLen; // "WEBP" + "VP8L" + size + payload(+pad)

        ByteBuffer out = ByteBuffer.allocate(8 + riffChunkSize).order(ByteOrder.LITTLE_ENDIAN);
        out.put((byte) 'R').put((byte) 'I').put((byte) 'F').put((byte) 'F');
        out.putInt(riffChunkSize);
        out.put((byte) 'W').put((byte) 'E').put((byte) 'B').put((byte) 'P');
        out.put((byte) 'V').put((byte) 'P').put((byte) '8').put((byte) 'L');
        out.putInt(vp8lPayload.length);
        out.put(vp8lPayload);
        if ((vp8lPayload.length & 1) != 0) {
            out.put((byte) 0);
        }
        return out.array();
    }

    /**
     * Writes a normal VP8L prefix code where symbols 0..255 all have code length 8.
     * Code-length code order: [17, 18, 0, 1, 2, 3, 4, 5, 6, 7, 8, ...].
     * Indices 2 (len 0) and 10 (len 8) get code length 1 (bit 0 = len 0, bit 1 = len 8).
     */
    private static void writeFixed8BitPrefixCode(BitWriter bits, boolean withMaxSymbol) {
        bits.writeBits(0, 1);  // is_simple = 0
        bits.writeBits(7, 4);  // num_code_lengths = 7 + 4 = 11 (indices 0..10)
        for (int i = 0; i < 11; i++) {
            bits.writeBits((i == 2 || i == 10) ? 1 : 0, 3);
        }
        if (withMaxSymbol) {
            bits.writeBits(1, 1);   // use_max_symbol = 1
            bits.writeBits(4, 3);   // length_nbits = 2 + 2*4 = 10
            bits.writeBits(254, 10); // max_symbol = 2 + 254 = 256
        } else {
            bits.writeBits(0, 1);   // use_max_symbol = 0
        }
        for (int s = 0; s < 256; s++) {
            bits.writeBits(1, 1);   // symbol s -> code length 8
        }
    }

    private static void writeDummyDistanceCode(BitWriter bits) {
        bits.writeBits(1, 1); // is_simple = 1
        bits.writeBits(0, 1); // num_symbols - 1 = 0 (1 symbol)
        bits.writeBits(0, 1); // is_first_8bits = 0
        bits.writeBits(0, 1); // symbol0 = 0
    }

    private static int reverse8(int value) {
        return Integer.reverse(value) >>> 24;
    }

    private static final class BitWriter {
        private final ByteArrayOutputStream out;
        private int currentByte = 0;
        private int bitCount = 0;

        BitWriter(int initialCapacity) {
            this.out = new ByteArrayOutputStream(initialCapacity);
        }

        void writeBits(int value, int numBits) {
            for (int i = 0; i < numBits; i++) {
                currentByte |= ((value >>> i) & 1) << bitCount;
                bitCount++;
                if (bitCount == 8) {
                    out.write(currentByte);
                    currentByte = 0;
                    bitCount = 0;
                }
            }
        }

        byte[] toByteArray() {
            if (bitCount > 0) {
                out.write(currentByte);
                currentByte = 0;
                bitCount = 0;
            }
            return out.toByteArray();
        }
    }
}
