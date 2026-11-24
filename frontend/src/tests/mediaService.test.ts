import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mediaService } from '../services/mediaService';
import { api } from '../services/api';

vi.mock('../services/api', () => ({
  api: {
    post: vi.fn(),
  },
}));

describe('mediaService WebP conversion', () => {
  const originalCreateImageBitmap = globalThis.createImageBitmap;
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.clearAllMocks();

    globalThis.createImageBitmap = vi.fn().mockResolvedValue({
      width: 200,
      height: 150,
      close: vi.fn(),
    } as unknown as ImageBitmap);

    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({
      drawImage: vi.fn(),
    } as unknown as CanvasRenderingContext2D);

    vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation((callback, type) => {
      const blob = new Blob(['RIFF\x00\x00\x00\x00WEBP'], { type: type || 'image/webp' });
      callback(blob);
    });
  });

  afterEach(() => {
    globalThis.createImageBitmap = originalCreateImageBitmap;
    globalThis.fetch = originalFetch;
    vi.restoreAllMocks();
  });

  it('converts a PNG file to WebP before requesting presigned URL and uploading to Object Storage', async () => {
    const pngFile = new File(['png-bytes'], 'avatar.png', { type: 'image/png' });

    vi.mocked(api.post)
      .mockResolvedValueOnce({
        data: {
          mediaId: 'media-1',
          storageKey: 'uploads/avatar/u1/avatar.webp',
          presignedUrl: 'http://localhost:9000/studywithus-media/uploads/avatar/u1/avatar.webp',
          expiresAt: '2026-11-24T00:00:00Z',
          requiredHeaders: { 'Content-Type': 'image/webp' },
        },
      })
      .mockResolvedValueOnce({
        data: {
          id: 'media-1',
          ownerId: 'u1',
          purpose: 'AVATAR',
          status: 'PROCESSED',
          storageKey: 'uploads/avatar/u1/avatar.webp',
          url: 'http://localhost:9000/studywithus-media/uploads/avatar/u1/avatar.webp',
          detectedMimeType: 'image/webp',
        },
      });

    globalThis.fetch = vi.fn().mockResolvedValue({ ok: true } as Response);

    const result = await mediaService.uploadImage(pngFile, 'AVATAR');

    expect(api.post).toHaveBeenNthCalledWith(1, '/uploads', expect.objectContaining({
      purpose: 'AVATAR',
      filename: 'avatar.webp',
      contentType: 'image/webp',
    }));

    const fetchCall = vi.mocked(globalThis.fetch).mock.calls[0];
    expect(fetchCall[1]?.method).toBe('PUT');
    expect((fetchCall[1]?.body as File).type).toBe('image/webp');
    expect((fetchCall[1]?.body as File).name).toBe('avatar.webp');
    expect(result.storageKey).toBe('uploads/avatar/u1/avatar.webp');
  });

  it('converts a JPEG file to WebP for check-in attachments', async () => {
    const jpgFile = new File(['jpg-bytes'], 'study-notes.jpg', { type: 'image/jpeg' });
    const converted = await mediaService.convertToWebP(jpgFile);

    expect(converted.type).toBe('image/webp');
    expect(converted.name).toBe('study-notes.webp');
  });

  it('keeps an already-WebP file as WebP without re-encoding', async () => {
    const webpFile = new File(['webp-bytes'], 'cover.webp', { type: 'image/webp' });
    const result = await mediaService.convertToWebP(webpFile);

    expect(result).toBe(webpFile);
    expect(globalThis.createImageBitmap).not.toHaveBeenCalled();
  });

  it('rejects unsupported file formats', async () => {
    const gifFile = new File(['gif-bytes'], 'anim.gif', { type: 'image/gif' });
    await expect(mediaService.convertToWebP(gifFile)).rejects.toThrow(
      'Formato inválido. Anexe uma imagem JPEG, PNG ou WebP.'
    );
  });
});
