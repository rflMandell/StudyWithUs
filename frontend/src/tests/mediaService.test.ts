import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { api } from '../services/api';
import { mediaService } from '../services/mediaService';

vi.mock('../services/api', () => ({
  api: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

vi.mock('axios', () => ({
  default: {
    put: vi.fn(),
  },
}));

describe('mediaService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('requests presigned URL, uploads file directly, and confirms upload', async () => {
    const mockFile = new File(['png-bytes'], 'notes.png', { type: 'image/png' });
    const uploadResponse = {
      mediaId: 'media-1',
      storageKey: 'uploads/checkin/u1/media-1.png',
      presignedUrl: 'http://localhost:8080/api/v1/media/storage/uploads/checkin/u1/media-1.png?sig=abc',
      expiresAt: '2026-11-19T15:00:00Z',
      requiredHeaders: { 'Content-Type': 'image/png' },
    };
    const confirmedAsset = {
      id: 'media-1',
      ownerId: 'u1',
      purpose: 'CHECKIN',
      status: 'CONFIRMED',
      storageKey: 'uploads/checkin/u1/media-1.png',
      url: 'http://localhost:8080/api/v1/media/public/uploads/checkin/u1/media-1.png',
      sizeBytes: 9,
      createdAt: '2026-11-19T14:55:00Z',
    };

    vi.mocked(api.post)
      .mockResolvedValueOnce({ data: uploadResponse })
      .mockResolvedValueOnce({ data: confirmedAsset });
    vi.mocked(axios.put).mockResolvedValueOnce({ status: 200 });

    const result = await mediaService.uploadImage(mockFile, 'CHECKIN');

    expect(api.post).toHaveBeenNthCalledWith(1, '/uploads', {
      purpose: 'CHECKIN',
      filename: 'notes.png',
      contentType: 'image/png',
      sizeBytes: mockFile.size,
      targetId: undefined,
    });
    expect(axios.put).toHaveBeenCalledWith(
      uploadResponse.presignedUrl,
      mockFile,
      { headers: { 'Content-Type': 'image/png' } }
    );
    expect(api.post).toHaveBeenNthCalledWith(2, '/uploads/media-1/confirm');
    expect(result).toEqual(confirmedAsset);
  });

  it('fetches media asset metadata by ID', async () => {
    const asset = { id: 'media-1', status: 'PROCESSED', storageKey: 'uploads/k.png' };
    vi.mocked(api.get).mockResolvedValueOnce({ data: asset });

    const result = await mediaService.getMediaAsset('media-1');

    expect(api.get).toHaveBeenCalledWith('/uploads/media-1');
    expect(result).toEqual(asset);
  });
});
