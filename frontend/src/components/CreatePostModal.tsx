import React, { useEffect, useRef, useState } from 'react';
import { Image, PenSquare, Send, X } from 'lucide-react';
import { checkinService } from '../services/checkinService';
import { mediaService } from '../services/mediaService';
import { generateUUID } from '../utils/uuid';
import { AlertError } from './AlertError';

interface CreatePostModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated: () => Promise<void> | void;
}

export const CreatePostModal: React.FC<CreatePostModalProps> = ({
  isOpen,
  onClose,
  onCreated,
}) => {
  const [caption, setCaption] = useState('');
  const [countsAsCheckin, setCountsAsCheckin] = useState(true);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const clearSelectedFile = () => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    setSelectedFile(null);
    setPreviewUrl(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  if (!isOpen) return null;

  const handleClose = () => {
    if (isSubmitting) return;
    setCaption('');
    setCountsAsCheckin(true);
    setError(null);
    clearSelectedFile();
    onClose();
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      mediaService.validateImageFile(file);
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Arquivo de imagem inválido.');
      e.target.value = '';
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!caption.trim() && !selectedFile) {
      setError('Adicione uma legenda ou anexe uma imagem para publicar.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      let mediaKeys: string[] = [];
      if (selectedFile) {
        const uploaded = await mediaService.uploadImage(selectedFile, 'CHECKIN');
        mediaKeys = [uploaded.storageKey];
      }

      await checkinService.createCheckin(
        {
          caption: caption.trim() || undefined,
          mediaKeys,
          countsAsCheckin,
        },
        generateUUID()
      );

      setCaption('');
      setCountsAsCheckin(true);
      clearSelectedFile();
      await onCreated();
    } catch (err: any) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.code ||
        err.message ||
        'Erro ao publicar post.';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Novo Post"
      onClick={handleClose}
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem',
        zIndex: 1000,
      }}
    >
      <div
        className="card"
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%',
          maxWidth: '540px',
          backgroundColor: 'var(--bg-card)',
          padding: '1.5rem',
          borderRadius: '12px',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.15)',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '1rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <PenSquare size={20} color="var(--primary)" />
            <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>Novo Post</h2>
          </div>
          <button
            type="button"
            onClick={handleClose}
            disabled={isSubmitting}
            aria-label="Fechar modal"
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '0.25rem',
            }}
          >
            <X size={20} />
          </button>
        </div>

        <AlertError message={error} />

        <form onSubmit={handleSubmit}>
          <textarea
            className="form-control"
            rows={4}
            placeholder="O que você estudou hoje? Compartilhe seu progresso..."
            value={caption}
            onChange={(e) => setCaption(e.target.value)}
            disabled={isSubmitting}
            style={{ resize: 'vertical', marginBottom: '0.75rem' }}
          />

          {previewUrl && (
            <div style={{ marginBottom: '0.75rem', position: 'relative', display: 'inline-block' }}>
              <img
                src={previewUrl}
                alt="Pré-visualização do post"
                style={{
                  maxHeight: '220px',
                  maxWidth: '100%',
                  borderRadius: '8px',
                  border: '1px solid var(--border)',
                  objectFit: 'cover',
                  display: 'block',
                }}
              />
              <button
                type="button"
                onClick={clearSelectedFile}
                disabled={isSubmitting}
                title="Remover imagem"
                aria-label="Remover imagem"
                style={{
                  position: 'absolute',
                  top: '6px',
                  right: '6px',
                  width: '26px',
                  height: '26px',
                  borderRadius: '50%',
                  backgroundColor: 'rgba(15, 23, 42, 0.75)',
                  color: '#ffffff',
                  border: 'none',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                }}
              >
                <X size={14} />
              </button>
            </div>
          )}

          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={handleFileSelect}
            style={{ display: 'none' }}
            aria-label="Anexar foto do post"
          />

          <label
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              marginBottom: '0.75rem',
              fontSize: '0.9rem',
              color: 'var(--text-main)',
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
              userSelect: 'none',
            }}
          >
            <input
              type="checkbox"
              checked={countsAsCheckin}
              onChange={(e) => setCountsAsCheckin(e.target.checked)}
              disabled={isSubmitting}
              style={{ width: '16px', height: '16px', accentColor: 'var(--primary)', cursor: 'pointer' }}
            />
            <span>Contar como check-in de estudo (+10 pts e sequência)</span>
          </label>

          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '0.75rem',
              marginTop: '0.5rem',
            }}
          >
            <button
              type="button"
              onClick={() => (selectedFile ? clearSelectedFile() : fileInputRef.current?.click())}
              disabled={isSubmitting}
              className="btn btn-secondary"
              style={{ width: 'auto', padding: '0.45rem 0.85rem', fontSize: '0.85rem', gap: '0.35rem' }}
            >
              <Image size={16} />
              <span>{selectedFile ? 'Remover Foto' : 'Anexar Foto'}</span>
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button
                type="button"
                onClick={handleClose}
                disabled={isSubmitting}
                className="btn btn-secondary"
                style={{ width: 'auto', padding: '0.5rem 1rem' }}
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="btn btn-primary"
                style={{ width: 'auto', padding: '0.5rem 1.25rem', gap: '0.4rem' }}
              >
                <Send size={16} />
                <span>{isSubmitting ? 'Publicando...' : 'Publicar'}</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
