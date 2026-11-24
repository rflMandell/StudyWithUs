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
const WEBP_QUALITY = 0.85;

function toWebpFilename(filename: string): string {
  const lastDot = filename.lastIndexOf('.');
  const baseName = lastDot > 0 ? filename.slice(0, lastDot) : filename;
  return `${baseName}.webp`;
}

async function decodeImage(file: File): Promise<{ width: number; height: number; draw: (ctx: CanvasRenderingContext2D) => void; close: () => void }> {
  if (typeof createImageBitmap === 'function') {
    const bitmap = await createImageBitmap(file);
    return {
      width: bitmap.width,
      height: bitmap.height,
      draw: (ctx) => ctx.drawImage(bitmap, 0, 0),
      close: () => bitmap.close(),
    };
  }

  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      resolve({
        width: img.naturalWidth || img.width,
        height: img.naturalHeight || img.height,
        draw: (ctx) => ctx.drawImage(img, 0, 0),
        close: () => URL.revokeObjectURL(url),
      });
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('Não foi possível decodificar a imagem para conversão em WebP.'));
    };
    img.src = url;
  });
}

export const mediaService = {
  validateImageFile(file: File): void {
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      throw new Error('Formato inválido. Anexe uma imagem JPEG, PNG ou WebP.');
    }
    if (file.size <= 0 || file.size > MAX_IMAGE_SIZE_BYTES) {
      throw new Error('A imagem deve ter no máximo 5 MB.');
    }
  },

  async convertToWebP(file: File): Promise<File> {
    this.validateImageFile(file);

    if (file.type === 'image/webp') {
      const normalizedName = toWebpFilename(file.name);
      if (file.name === normalizedName) {
        return file;
      }
      return new File([file], normalizedName, {
        type: 'image/webp',
        lastModified: file.lastModified,
      });
    }

    const decoded = await decodeImage(file);
    try {
      const canvas = document.createElement('canvas');
      canvas.width = decoded.width;
      canvas.height = decoded.height;

      const ctx = canvas.getContext('2d');
      if (!ctx) {
        throw new Error('Contexto 2D indisponível para converter a imagem em WebP.');
      }

      decoded.draw(ctx);

      const blob = await new Promise<Blob | null>((resolve) => {
        canvas.toBlob(resolve, 'image/webp', WEBP_QUALITY);
      });

      if (!blob) {
        throw new Error('Falha ao converter a imagem para o formato WebP.');
      }

      if (blob.size <= 0 || blob.size > MAX_IMAGE_SIZE_BYTES) {
        throw new Error('A imagem convertida para WebP excede o limite de 5 MB.');
      }

      return new File([blob], toWebpFilename(file.name), {
        type: 'image/webp',
        lastModified: Date.now(),
      });
    } finally {
      decoded.close();
    }
  },

  async uploadImage(file: File, purpose: MediaPurpose, targetId?: string): Promise<MediaAssetResponse> {
    const webpFile = await this.convertToWebP(file);

    const { data: presigned } = await api.post<UploadUrlResponse>('/uploads', {
      purpose,
      filename: webpFile.name,
      contentType: webpFile.type,
      sizeBytes: webpFile.size,
      targetId,
    });

    const uploadResponse = await fetch(presigned.presignedUrl, {
      method: 'PUT',
      headers: presigned.requiredHeaders || { 'Content-Type': webpFile.type },
      body: webpFile,
    });

    if (!uploadResponse.ok) {
      throw new Error('Falha ao enviar a imagem para o armazenamento S3.');
    }

    const { data: confirmed } = await api.post<MediaAssetResponse>(`/uploads/${presigned.mediaId}/confirm`);
    return confirmed;
  },
};
