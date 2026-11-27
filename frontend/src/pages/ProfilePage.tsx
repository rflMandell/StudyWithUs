import React, { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  Award,
  Camera,
  CheckCircle2,
  Edit3,
  Flame,
  Lock,
  School,
  Sparkles,
  Upload,
  Users,
  X,
  Save,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { Navbar } from '../components/Navbar';
import { getAvatarBorderStyle } from '../utils/levelCosmetics';
import { User, UpdateUserData, UserBadge } from '../types/auth';
import { userService } from '../services/userService';
import { mediaService } from '../services/mediaService';
import { AlertError } from '../components/AlertError';

export const ProfilePage: React.FC = () => {
  const { user: authUser, updateUser } = useAuth();
  const { userId } = useParams<{ userId?: string }>();

  const isOwnProfile = !userId || userId === authUser?.id;

  const [profile, setProfile] = useState<User | null>(null);
  const [badges, setBadges] = useState<UserBadge[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const avatarInputRef = useRef<HTMLInputElement>(null);

  // Form state
  const [formData, setFormData] = useState<UpdateUserData>({
    name: '',
    bio: '',
    institution: '',
    course: '',
    studyGoal: '',
    birthDate: '',
  });

  const loadProfile = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const targetId = isOwnProfile ? authUser?.id : userId;
      const data = await userService.getProfile(targetId);
      setProfile(data);

      setFormData({
        name: data.name || '',
        bio: data.bio || '',
        institution: data.institution || '',
        course: data.course || '',
        studyGoal: data.studyGoal || '',
        birthDate: data.birthDate || '',
      });

      if (data.badges) {
        setBadges(data.badges);
      } else if (data.id) {
        const badgeList = await userService.getUserBadges(data.id);
        setBadges(badgeList);
      }
    } catch (err: any) {
      console.error('Erro ao carregar perfil:', err);
      setError('Não foi possível carregar os dados do perfil.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, [userId, authUser?.id]);

  useEffect(() => {
    if (!isAvatarModalOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsAvatarModalOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isAvatarModalOpen]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleAvatarFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;

    setIsUploadingAvatar(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const uploaded = await mediaService.uploadImage(file, 'AVATAR');
      const updated = await userService.uploadAvatar(uploaded.id);
      setProfile(updated);
      updateUser(updated);
      setSuccessMessage('Foto de perfil atualizada com sucesso!');
    } catch (err: any) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.code ||
        err.message ||
        'Erro ao enviar foto de perfil.';
      setError(msg);
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name?.trim()) {
      setError('O nome é obrigatório.');
      return;
    }

    setIsSaving(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const updated = await userService.updateProfile({
        name: formData.name.trim(),
        bio: formData.bio?.trim() || undefined,
        institution: formData.institution?.trim() || undefined,
        course: formData.course?.trim() || undefined,
        studyGoal: formData.studyGoal?.trim() || undefined,
        birthDate: formData.birthDate || undefined,
      });

      setProfile(updated);
      updateUser(updated);
      setIsEditing(false);
      setSuccessMessage('Perfil atualizado com sucesso!');
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.code || 'Erro ao atualizar perfil.';
      setError(msg);
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div>
        <Navbar />
        <main className="container" style={{ padding: '3rem 0', textAlign: 'center' }}>
          <p style={{ color: 'var(--text-muted)' }}>Carregando dados do perfil...</p>
        </main>
      </div>
    );
  }

  if (!profile) {
    return (
      <div>
        <Navbar />
        <main className="container" style={{ padding: '3rem 0', textAlign: 'center' }}>
          <p style={{ color: 'var(--danger)' }}>Perfil não encontrado.</p>
        </main>
      </div>
    );
  }

  const isRestrictedPrivateProfile = !isOwnProfile && !!profile.isPrivate;

  return (
    <div>
      <Navbar />
      <main className="container">
        {error && <AlertError message={error} />}
        {successMessage && (
          <div
            style={{
              padding: '0.75rem 1rem',
              backgroundColor: '#ecfdf5',
              color: '#065f46',
              border: '1px solid #a7f3d0',
              borderRadius: '6px',
              marginBottom: '1.5rem',
              fontSize: '0.9rem',
            }}
          >
            {successMessage}
          </div>
        )}

        {/* Profile Card Header */}
        <div className="card" style={{ marginBottom: '2rem' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: '1.5rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
              <div style={{ position: 'relative', flexShrink: 0 }}>
                <div
                  role={!isEditing && profile.avatarUrl ? 'button' : undefined}
                  tabIndex={!isEditing && profile.avatarUrl ? 0 : undefined}
                  title={!isEditing && profile.avatarUrl ? 'Ampliar foto de perfil' : undefined}
                  aria-label={!isEditing && profile.avatarUrl ? 'Ampliar foto de perfil' : undefined}
                  onClick={() => {
                    if (!isEditing && profile.avatarUrl) {
                      setIsAvatarModalOpen(true);
                    }
                  }}
                  onKeyDown={(e) => {
                    if (!isEditing && profile.avatarUrl && (e.key === 'Enter' || e.key === ' ')) {
                      e.preventDefault();
                      setIsAvatarModalOpen(true);
                    }
                  }}
                  style={{
                    width: '76px',
                    height: '76px',
                    borderRadius: '50%',
                    backgroundColor: 'var(--primary-subtle, #eff6ff)',
                    color: 'var(--primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 700,
                    fontSize: '2rem',
                    overflow: 'hidden',
                    border: getAvatarBorderStyle(profile.activeAvatarBorder),
                    cursor: !isEditing && profile.avatarUrl ? 'pointer' : 'default',
                  }}
                >
                  {profile.avatarUrl ? (
                    <img
                      src={profile.avatarUrl}
                      alt={profile.name}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  ) : (
                    profile.name.charAt(0).toUpperCase()
                  )}
                </div>
                {isOwnProfile && isEditing && (
                  <>
                    <input
                      ref={avatarInputRef}
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      onChange={handleAvatarFileChange}
                      style={{ display: 'none' }}
                      aria-label="Selecionar foto de perfil"
                    />
                    <button
                      type="button"
                      onClick={() => avatarInputRef.current?.click()}
                      disabled={isUploadingAvatar}
                      title="Alterar foto de perfil"
                      style={{
                        position: 'absolute',
                        bottom: 0,
                        right: 0,
                        width: '28px',
                        height: '28px',
                        borderRadius: '50%',
                        backgroundColor: 'var(--primary)',
                        color: '#ffffff',
                        border: '2px solid #ffffff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        cursor: isUploadingAvatar ? 'wait' : 'pointer',
                      }}
                    >
                      <Camera size={14} />
                    </button>
                  </>
                )}
              </div>

              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
                  <h1 style={{ fontSize: '1.75rem', fontWeight: 700, letterSpacing: '-0.025em' }}>
                    {profile.name}
                  </h1>
                  {!isRestrictedPrivateProfile && (
                    <>
                      <span
                        style={{
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          backgroundColor: '#ecfdf5',
                          color: '#059669',
                          padding: '0.15rem 0.5rem',
                          borderRadius: '9999px',
                        }}
                      >
                        Ativo
                      </span>
                      {profile.level != null && (
                        <span
                          style={{
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: '#eef2ff',
                            color: '#4338ca',
                            padding: '0.15rem 0.55rem',
                            borderRadius: '9999px',
                          }}
                        >
                          Nível {profile.level} {profile.activeTitle ? `• ${profile.activeTitle}` : ''}
                        </span>
                      )}
                    </>
                  )}
                </div>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', marginTop: '0.15rem' }}>
                  @{profile.username}
                </p>
                {!isRestrictedPrivateProfile && profile.level != null && profile.nextLevelXp != null && (
                  <div style={{ marginTop: '0.6rem', maxWidth: '320px' }}>
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        fontSize: '0.75rem',
                        color: 'var(--text-muted)',
                        marginBottom: '0.25rem',
                      }}
                    >
                      <span>XP: {profile.currentLevelXp ?? 0} / {profile.nextLevelXp}</span>
                      <span>{profile.progressPercent ?? 0}%</span>
                    </div>
                    <div
                      style={{
                        width: '100%',
                        height: '8px',
                        backgroundColor: 'var(--bg-secondary)',
                        borderRadius: '9999px',
                        overflow: 'hidden',
                      }}
                    >
                      <div
                        style={{
                          width: `${Math.min(Math.max(profile.progressPercent ?? 0, 0), 100)}%`,
                          height: '100%',
                          backgroundColor: 'var(--primary)',
                          borderRadius: '9999px',
                          transition: 'width 0.3s ease',
                        }}
                      />
                    </div>
                  </div>
                )}
                {profile.bio && (
                  <p style={{ marginTop: '0.5rem', fontSize: '0.95rem', color: 'var(--text-main)', maxWidth: '600px' }}>
                    {profile.bio}
                  </p>
                )}
              </div>
            </div>

            {isOwnProfile && (
              <button
                onClick={() => setIsEditing(!isEditing)}
                className={isEditing ? 'btn btn-secondary' : 'btn btn-primary'}
                style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.9rem' }}
              >
                {isEditing ? (
                  <>
                    <X size={16} />
                    Cancelar
                  </>
                ) : (
                  <>
                    <Edit3 size={16} />
                    Editar Perfil
                  </>
                )}
              </button>
            )}
          </div>

          {/* Edit Form */}
          {isEditing && (
            <form
              onSubmit={handleSaveProfile}
              style={{
                marginTop: '1.5rem',
                paddingTop: '1.5rem',
                borderTop: '1px solid var(--border)',
                display: 'flex',
                flexDirection: 'column',
                gap: '1rem',
              }}
            >
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600 }}>Editar Informações Pessoais</h3>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '0.25rem' }}>
                    Nome Completo *
                  </label>
                  <input
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleInputChange}
                    className="input"
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '0.25rem' }}>
                    Instituição de Ensino
                  </label>
                  <input
                    type="text"
                    name="institution"
                    value={formData.institution}
                    onChange={handleInputChange}
                    placeholder="Ex: USP, Unicamp, IFSP"
                    className="input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '0.25rem' }}>
                    Curso
                  </label>
                  <input
                    type="text"
                    name="course"
                    value={formData.course}
                    onChange={handleInputChange}
                    placeholder="Ex: Ciência da Computação"
                    className="input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '0.25rem' }}>
                    Meta de Estudo
                  </label>
                  <input
                    type="text"
                    name="studyGoal"
                    value={formData.studyGoal}
                    onChange={handleInputChange}
                    placeholder="Ex: Passar na residência, Estágio em tech"
                    className="input"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '0.25rem' }}>
                    Foto de Perfil
                  </label>
                  <button
                    type="button"
                    onClick={() => avatarInputRef.current?.click()}
                    disabled={isUploadingAvatar}
                    className="btn btn-secondary"
                    style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.4rem' }}
                  >
                    <Upload size={15} />
                    <span>{isUploadingAvatar ? 'Enviando foto...' : 'Anexar Imagem (JPEG, PNG, WebP)'}</span>
                  </button>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '0.25rem' }}>
                    Data de Nascimento
                  </label>
                  <input
                    type="date"
                    name="birthDate"
                    value={formData.birthDate}
                    onChange={handleInputChange}
                    className="input"
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '0.25rem' }}>
                  Biografia
                </label>
                <textarea
                  name="bio"
                  value={formData.bio}
                  onChange={handleInputChange}
                  rows={3}
                  placeholder="Conte um pouco sobre sua rotina e objetivos acadêmicos..."
                  className="input"
                  style={{ width: '100%', resize: 'vertical' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  onClick={() => setIsEditing(false)}
                  disabled={isSaving}
                  className="btn btn-secondary"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="btn btn-primary"
                  style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
                >
                  <Save size={16} />
                  {isSaving ? 'Salvando...' : 'Salvar Alterações'}
                </button>
              </div>
            </form>
          )}
        </div>

        {isRestrictedPrivateProfile ? (
          <div
            className="card"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              padding: '1.5rem',
              color: 'var(--text-muted)',
            }}
          >
            <Lock size={20} color="var(--primary)" />
            <p style={{ fontWeight: 500 }}>
              Conta Privada — Apenas informações básicas estão visíveis
            </p>
          </div>
        ) : (
          <>
        {/* Consolidated Statistics Grid */}
        <div className="grid-cards" style={{ marginBottom: '2rem' }}>
          {/* Points */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: 'var(--primary)', marginBottom: '0.5rem' }}>
              <Award size={22} />
              <h3>Pontuação</h3>
            </div>
            <p style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--primary)' }}>
              {profile.currentPoints ?? profile.lifetimePoints}{' '}
              <span style={{ fontSize: '1rem', fontWeight: 500, color: 'var(--text-muted)' }}>pontos</span>
            </p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.4rem' }}>
              Total vitalício: {profile.lifetimePoints} pts
            </p>
          </div>

          {/* Communities */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: '#0284c7', marginBottom: '0.5rem' }}>
              <Users size={22} />
              <h3>Comunidades</h3>
            </div>
            <p style={{ fontSize: '2rem', fontWeight: 700, color: '#0284c7' }}>
              {profile.activeCommunitiesCount ?? 0}
            </p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.4rem' }}>
              Grupos acadêmicos ativos
            </p>
          </div>

          {/* Checkins */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: '#16a34a', marginBottom: '0.5rem' }}>
              <CheckCircle2 size={22} />
              <h3>Check-ins de Estudo</h3>
            </div>
            <p style={{ fontSize: '2rem', fontWeight: 700, color: '#16a34a' }}>
              {profile.checkinsCount ?? 0}
            </p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.4rem' }}>
              Atividades de estudo publicadas
            </p>
          </div>

          {/* Streak */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: '#ea580c', marginBottom: '0.5rem' }}>
              <Flame size={22} />
              <h3>Streak de Estudos</h3>
            </div>
            <p style={{ fontSize: '2rem', fontWeight: 700, color: '#ea580c' }}>
              {profile.currentStreak ?? 0}{' '}
              <span style={{ fontSize: '1rem', fontWeight: 500, color: 'var(--text-muted)' }}>dias</span>
            </p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.4rem' }}>
              Maior sequência: {profile.longestStreak ?? 0} dias
            </p>
          </div>
        </div>

        {/* Academic Details & Goals */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem', color: '#0284c7' }}>
              <School size={20} />
              <h2 style={{ fontSize: '1.15rem', fontWeight: 600 }}>Informações Acadêmicas</h2>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', fontSize: '0.95rem' }}>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Instituição</span>
                <span style={{ fontWeight: 600 }}>{profile.institution || 'Não informada'}</span>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Curso</span>
                <span style={{ fontWeight: 600 }}>{profile.course || 'Não informado'}</span>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Meta de Estudo</span>
                <span style={{ fontWeight: 600 }}>{profile.studyGoal || 'Não informada'}</span>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.8rem' }}>Membro desde</span>
                <span style={{ fontWeight: 500 }}>{new Date(profile.createdAt).toLocaleDateString()}</span>
              </div>
            </div>
          </div>

          {/* Badges / Conquistas */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem', color: '#d97706' }}>
              <Sparkles size={20} />
              <h2 style={{ fontSize: '1.15rem', fontWeight: 600 }}>Conquistas & Badges</h2>
            </div>

            {badges.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '1.5rem 0' }}>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '0.5rem' }}>
                  Nenhuma badge conquistada ainda.
                </p>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  Conclua desafios e mantenha sua consistência de check-ins para desbloquear medalhas!
                </p>
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '0.75rem' }}>
                {badges.map((badge) => (
                  <div
                    key={badge.badgeId}
                    style={{
                      border: '1px solid var(--warning-border)',
                      borderRadius: '8px',
                      padding: '0.75rem',
                      textAlign: 'center',
                      backgroundColor: 'var(--warning-light)',
                    }}
                  >
                    <div style={{ fontSize: '1.75rem', marginBottom: '0.25rem' }}>🏆</div>
                    <p style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--warning-text)' }}>{badge.name}</p>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                      {badge.description}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
          </>
        )}
      </main>

      {isAvatarModalOpen && profile.avatarUrl && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={`Foto de perfil de ${profile.name}`}
          onClick={() => setIsAvatarModalOpen(false)}
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 1000,
            backgroundColor: 'rgba(0, 0, 0, 0.85)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1.5rem',
            cursor: 'pointer',
          }}
        >
          <button
            type="button"
            onClick={() => setIsAvatarModalOpen(false)}
            aria-label="Fechar visualização"
            style={{
              position: 'absolute',
              top: '1.25rem',
              right: '1.25rem',
              width: '40px',
              height: '40px',
              borderRadius: '50%',
              backgroundColor: 'rgba(255, 255, 255, 0.15)',
              color: '#ffffff',
              border: 'none',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            <X size={20} />
          </button>
          <img
            src={profile.avatarUrl}
            alt={profile.name}
            onClick={(e) => e.stopPropagation()}
            style={{
              maxWidth: '90vw',
              maxHeight: '85vh',
              borderRadius: '12px',
              objectFit: 'contain',
              boxShadow: '0 20px 50px rgba(0, 0, 0, 0.5)',
              cursor: 'default',
            }}
          />
        </div>
      )}
    </div>
  );
};
