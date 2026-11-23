import { api } from './api';

export type MediaPurpose = 'AVATAR' | 'CHECKIN' | 'COMMUNITY_COVER';

export interface UploadUrlResponse {
  mediaId: string;
  storageKey: string;
  presignedUrl: string;
  expiresAt: string;
  requiredHeaders: Record<string, string>;
}

export interface MediaAssetResponse {
  id: string;
  ownerId: string;
  purpose: MediaPurpose;
  status: string;
  storageKey: string;
  thumbnailKey?: string | null;
  url: string;
  thumbnailUrl?: string | null;
  detectedMimeType?: string | null;
  sizeBytes?: number | null;
}

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

export const mediaService = {
  validateImageFile(file: File): void {
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      throw new Error('Formato inválido. Anexe uma imagem JPEG, PNG ou WebP.');
    }
    if (file.size <= 0 || file.size > MAX_IMAGE_SIZE_BYTES) {
      throw new Error('A imagem deve ter no máximo 5 MB.');
    }
  },

  async uploadImage(file: File, purpose: MediaPurpose, targetId?: string): Promise<MediaAssetResponse> {
    this.validateImageFile(file);

    const { data: presigned } = await api.post<UploadUrlResponse>('/uploads', {
      purpose,
      filename: file.name,
      contentType: file.type,
      sizeBytes: file.size,
      targetId,
    });

    const uploadResponse = await fetch(presigned.presignedUrl, {
      method: 'PUT',
      headers: presigned.requiredHeaders || { 'Content-Type': file.type },
      body: file,
    });

    if (!uploadResponse.ok) {
      throw new Error('Falha ao enviar a imagem para o armazenamento S3.');
    }

    const { data: confirmed } = await api.post<MediaAssetResponse>(`/uploads/${presigned.mediaId}/confirm`);
    return confirmed;
  },
};
