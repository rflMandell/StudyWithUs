import React, { useState } from 'react';
import { X, Users, Globe, Lock } from 'lucide-react';
import { CommunityVisibility, CreateCommunityData, Community } from '../types/community';
import { communityService } from '../services/communityService';
import { AlertError } from './AlertError';

interface CreateCommunityModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (community: Community) => void;
}

export const CreateCommunityModal: React.FC<CreateCommunityModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [visibility, setVisibility] = useState<CommunityVisibility>('PUBLIC');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!name.trim()) {
      setError('O nome da comunidade é obrigatório.');
      return;
    }

    setIsLoading(true);
    try {
      const payload: CreateCommunityData = {
        name: name.trim(),
        description: description.trim() || undefined,
        visibility,
        timezone: 'America/Sao_Paulo',
      };

      const created = await communityService.createCommunity(payload);
      setName('');
      setDescription('');
      setVisibility('PUBLIC');
      onSuccess(created);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao criar comunidade.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.6)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem',
        zIndex: 50,
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '520px',
          maxHeight: '90vh',
          overflowY: 'auto',
          position: 'relative',
        }}
      >
        <button
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '1.25rem',
            right: '1.25rem',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: 'var(--text-muted)',
          }}
        >
          <X size={20} />
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem', color: 'var(--primary)' }}>
          <Users size={24} />
          <h2 style={{ fontSize: '1.35rem', fontWeight: 700 }}>Nova Comunidade de Estudo</h2>
        </div>

        <AlertError message={error} />

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="communityName">Nome da Comunidade *</label>
            <input
              id="communityName"
              type="text"
              className="form-control"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ex: Grupo de Cálculo 1 — USP"
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="communityDesc">Descrição / Propósito</label>
            <textarea
              id="communityDesc"
              className="form-control"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Descreva o foco da comunidade, matérias e dinâmica de estudos..."
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label>Visibilidade</label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginTop: '0.3rem' }}>
              <button
                type="button"
                onClick={() => setVisibility('PUBLIC')}
                className={`btn ${visibility === 'PUBLIC' ? 'btn-primary' : 'btn-secondary'}`}
                style={{ padding: '0.6rem', fontSize: '0.875rem', gap: '0.4rem' }}
              >
                <Globe size={16} />
                <span>Pública</span>
              </button>

              <button
                type="button"
                onClick={() => setVisibility('PRIVATE')}
                className={`btn ${visibility === 'PRIVATE' ? 'btn-primary' : 'btn-secondary'}`}
                style={{ padding: '0.6rem', fontSize: '0.875rem', gap: '0.4rem' }}
              >
                <Lock size={16} />
                <span>Privada</span>
              </button>
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.4rem' }}>
              {visibility === 'PUBLIC'
                ? 'Qualquer aluno pode encontrar e ingressar diretamente.'
                : 'Exige aprovação de um administrador para novos membros entrarem.'}
            </p>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.75rem' }}>
            <button
              type="button"
              onClick={onClose}
              className="btn btn-secondary"
              style={{ width: 'auto' }}
              disabled={isLoading}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: 'auto' }}
              disabled={isLoading}
            >
              {isLoading ? 'Criando...' : 'Criar Comunidade'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
