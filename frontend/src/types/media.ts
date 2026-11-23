export type MediaPurpose = 'CHECKIN' | 'AVATAR' | 'COMMUNITY_COVER';
export type MediaStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSED' | 'REJECTED' | 'DELETED';

export interface RequestUploadUrlPayload {
  purpose: MediaPurpose;
  filename: string;
  contentType: string;
  sizeBytes: number;
  targetId?: string;
}

export interface UploadUrlResponse {
  mediaId: string;
  storageKey: string;
  presignedUrl: string;
  expiresAt: string;
  requiredHeaders: Record<string, string>;
}

export interface MediaAsset {
  id: string;
  ownerId: string;
  purpose: MediaPurpose;
  status: MediaStatus;
  storageKey: string;
  thumbnailKey?: string;
  url?: string;
  thumbnailUrl?: string;
  detectedMimeType?: string;
  sizeBytes: number;
  width?: number;
  height?: number;
  createdAt: string;
  confirmedAt?: string;
  processedAt?: string;
}
