import axios from 'axios';
import { api } from './api';
import { MediaAsset, MediaPurpose, RequestUploadUrlPayload, UploadUrlResponse } from '../types/media';

export const mediaService = {
  async requestUploadUrl(payload: RequestUploadUrlPayload): Promise<UploadUrlResponse> {
    const response = await api.post<UploadUrlResponse>('/uploads', payload);
    return response.data;
  },

  async uploadToPresignedUrl(presignedUrl: string, file: File, requiredHeaders: Record<string, string> = {}): Promise<void> {
    await axios.put(presignedUrl, file, {
      headers: {
        'Content-Type': file.type,
        ...requiredHeaders,
      },
    });
  },

  async confirmUpload(mediaId: string): Promise<MediaAsset> {
    const response = await api.post<MediaAsset>(`/uploads/${mediaId}/confirm`);
    return response.data;
  },

  async getMediaAsset(mediaId: string): Promise<MediaAsset> {
    const response = await api.get<MediaAsset>(`/uploads/${mediaId}`);
    return response.data;
  },

  async uploadImage(file: File, purpose: MediaPurpose, targetId?: string): Promise<MediaAsset> {
    const uploadInfo = await this.requestUploadUrl({
      purpose,
      filename: file.name,
      contentType: file.type,
      sizeBytes: file.size,
      targetId,
    });
    await this.uploadToPresignedUrl(uploadInfo.presignedUrl, file, uploadInfo.requiredHeaders);
    return this.confirmUpload(uploadInfo.mediaId);
  },
};
