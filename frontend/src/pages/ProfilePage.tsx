import React, { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Award,
  BookOpen,
  Camera,
  Check,
  CheckCircle2,
  Clock,
  Edit3,
  Flame,
  Lock,
  School,
  Sparkles,
  Upload,
  UserCheck,
  UserPlus,
  Users,
  X,
  Save,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { Navbar } from '../components/Navbar';
import { getAvatarBorderStyle } from '../utils/levelCosmetics';
import { FollowRequestItem, FollowStatus, User, UpdateUserData, UserBadge } from '../types/auth';
import { FeedItem } from '../types/feed';
import { userService } from '../services/userService';
import { followService } from '../services/followService';
import { mediaService } from '../services/mediaService';
import { AlertError } from '../components/AlertError';

export const ProfilePage: React.FC = () => {
  const { user: authUser, updateUser } = useAuth();
  const { userId } = useParams<{ userId?: string }>();

  const isOwnProfile = !userId || userId === authUser?.id;

  const [profile, setProfile] = useState<User | null>(null);
  const [badges, setBadges] = useState<UserBadge[]>([]);
  const [checkins, setCheckins] = useState<FeedItem[]>([]);
  const [isLoadingCheckins, setIsLoadingCheckins] = useState(false);
  const [followStatus, setFollowStatus] = useState<FollowStatus | null>(null);
  const [followersCount, setFollowersCount] = useState<number | null>(null);
  const [followingCount, setFollowingCount] = useState<number | null>(null);
  const [isFollowLoading, setIsFollowLoading] = useState(false);
  const [pendingRequests, setPendingRequests] = useState<FollowRequestItem[]>([]);
  const [processingRequestId, setProcessingRequestId] = useState<string | null>(null);
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
    setFollowStatus(null);
    setFollowersCount(null);
    setFollowingCount(null);
    setPendingRequests([]);
    setCheckins([]);
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

      let resolvedStatus: FollowStatus | null = null;
      if (!isOwnProfile && data.id) {
        if (typeof followService?.getFollowStatus === 'function') {
          try {
            const statusRes = await followService.getFollowStatus(data.id);
            if (statusRes) {
              resolvedStatus = statusRes.status;
              setFollowStatus(statusRes.status);
              setFollowersCount(statusRes.followersCount);
              setFollowingCount(statusRes.followingCount);
            } else {
              resolvedStatus = 'NONE';
              setFollowStatus('NONE');
            }
          } catch {
            resolvedStatus = 'NONE';
            setFollowStatus('NONE');
          }
        } else {
          resolvedStatus = 'NONE';
          setFollowStatus('NONE');
        }
      }

      const restricted = !isOwnProfile && !!data.isPrivate && resolvedStatus !== 'FOLLOWING';

      if (!restricted) {
        if (data.badges) {
          setBadges(data.badges);
        } else if (data.id && typeof userService.getUserBadges === 'function') {
          const badgeList = await userService.getUserBadges(data.id);
          setBadges(badgeList);
        }

        if (data.id && typeof userService.getUserCheckins === 'function') {
          setIsLoadingCheckins(true);
          try {
            const userCheckins = await userService.getUserCheckins(data.id);
            setCheckins(Array.isArray(userCheckins) ? userCheckins : []);
          } catch {
            setCheckins([]);
          } finally {
            setIsLoadingCheckins(false);
          }
        }
      } else {
        setBadges([]);
      }

      if (isOwnProfile && typeof followService?.getPendingRequests === 'function') {
        try {
          const requests = await followService.getPendingRequests();
          setPendingRequests(Array.isArray(requests) ? requests : []);
        } catch {
          setPendingRequests([]);
        }
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

  const handleFollowToggle = async () => {
    if (!profile || isFollowLoading) return;
    setIsFollowLoading(true);
    setError(null);
    try {
      if (followStatus === 'FOLLOWING' || followStatus === 'REQUESTED') {
        const res = await followService.unfollowUser(profile.id);
        const nextStatus = res?.status ?? 'NONE';
        setFollowStatus(nextStatus);
        if (res?.followersCount != null) setFollowersCount(res.followersCount);
        if (res?.followingCount != null) setFollowingCount(res.followingCount);
        if (profile.isPrivate) {
          setCheckins([]);
          setBadges([]);
        }
      } else {
        const res = await followService.followUser(profile.id);
        const nextStatus = res?.status ?? (profile.isPrivate ? 'REQUESTED' : 'FOLLOWING');
        setFollowStatus(nextStatus);
        if (res?.followersCount != null) setFollowersCount(res.followersCount);
        if (res?.followingCount != null) setFollowingCount(res.followingCount);
        if (nextStatus === 'FOLLOWING' && typeof userService.getUserCheckins === 'function') {
          setIsLoadingCheckins(true);
          try {
            const userCheckins = await userService.getUserCheckins(profile.id);
            setCheckins(Array.isArray(userCheckins) ? userCheckins : []);
          } catch {
            setCheckins([]);
          } finally {
            setIsLoadingCheckins(false);
          }
        }
      }
    } catch {
      setError('Não foi possível atualizar o status de seguidor.');
    } finally {
      setIsFollowLoading(false);
    }
  };

  const handleAcceptRequest = async (requestId: string) => {
    if (processingRequestId) return;
    setProcessingRequestId(requestId);
    setError(null);
    try {
      await followService.acceptRequest(requestId);
      setPendingRequests((prev) => prev.filter((req) => req.id !== requestId));
    } catch {
      setError('Não foi possível aceitar a solicitação.');
    } finally {
      setProcessingRequestId(null);
    }
  };

  const handleRejectRequest = async (requestId: string) => {
    if (processingRequestId) return;
    setProcessingRequestId(requestId);
    setError(null);
    try {
      await followService.rejectRequest(requestId);
      setPendingRequests((prev) => prev.filter((req) => req.id !== requestId));
    } catch {
      setError('Não foi possível recusar a solicitação.');
    } finally {
      setProcessingRequestId(null);
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

  const isRestrictedPrivateProfile = !isOwnProfile && !!profile.isPrivate && followStatus !== 'FOLLOWING';

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
                {followersCount !== null && followingCount !== null && (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.25rem' }}>
                    <strong style={{ color: 'var(--text-main)' }}>{followersCount}</strong> seguidores •{' '}
                    <strong style={{ color: 'var(--text-main)' }}>{followingCount}</strong> seguindo
                  </p>
                )}
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

            {isOwnProfile ? (
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
            ) : (
              followStatus &&
              followStatus !== 'SELF' && (
                <button
                  type="button"
                  onClick={handleFollowToggle}
                  disabled={isFollowLoading}
                  className={followStatus === 'NONE' ? 'btn btn-primary' : 'btn btn-secondary'}
                  style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.9rem', width: 'auto' }}
                >
                  {followStatus === 'FOLLOWING' ? (
                    <>
                      <UserCheck size={16} />
                      <span>Seguindo</span>
                    </>
                  ) : followStatus === 'REQUESTED' ? (
                    <>
                      <Clock size={16} />
                      <span>Solicitado</span>
                    </>
                  ) : (
                    <>
                      <UserPlus size={16} />
                      <span>{profile.isPrivate ? 'Solicitar para seguir' : 'Seguir'}</span>
                    </>
                  )}
                </button>
              )
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

        {/* Pending Follow Requests (Own Profile) */}
        {isOwnProfile && pendingRequests.length > 0 && (
          <div className="card" style={{ marginBottom: '2rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1rem', color: 'var(--primary)' }}>
              <UserPlus size={20} />
              <h2 style={{ fontSize: '1.15rem', fontWeight: 600 }}>Solicitações para seguir</h2>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {pendingRequests.map((req) => (
                <div
                  key={req.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    gap: '0.75rem',
                    padding: '0.75rem',
                    border: '1px solid var(--border)',
                    borderRadius: '8px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div
                      style={{
                        width: '40px',
                        height: '40px',
                        borderRadius: '50%',
                        backgroundColor: 'var(--primary-light)',
                        color: 'var(--primary)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 700,
                        overflow: 'hidden',
                        flexShrink: 0,
                      }}
                    >
                      {req.requesterAvatarUrl ? (
                        <img
                          src={req.requesterAvatarUrl}
                          alt={req.requesterName}
                          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                        />
                      ) : (
                        req.requesterName.charAt(0).toUpperCase()
                      )}
                    </div>
                    <div>
                      <p style={{ fontWeight: 600, fontSize: '0.95rem' }}>{req.requesterName}</p>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>@{req.requesterUsername}</p>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button
                      type="button"
                      onClick={() => handleAcceptRequest(req.id)}
                      disabled={processingRequestId === req.id}
                      className="btn btn-primary"
                      style={{ width: 'auto', padding: '0.4rem 0.85rem', fontSize: '0.85rem', gap: '0.3rem' }}
                    >
                      <Check size={14} />
                      <span>Aceitar</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => handleRejectRequest(req.id)}
                      disabled={processingRequestId === req.id}
                      className="btn btn-secondary"
                      style={{ width: 'auto', padding: '0.4rem 0.85rem', fontSize: '0.85rem', gap: '0.3rem' }}
                    >
                      <X size={14} />
                      <span>Recusar</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

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

        {/* User Study Check-ins */}
        <div className="card" style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem', color: 'var(--primary)' }}>
            <BookOpen size={20} />
            <h2 style={{ fontSize: '1.15rem', fontWeight: 600 }}>Publicações de Estudo</h2>
          </div>

          {isLoadingCheckins ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', textAlign: 'center', padding: '1.5rem 0' }}>
              Carregando publicações...
            </p>
          ) : checkins.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', textAlign: 'center', padding: '1.5rem 0' }}>
              Nenhuma publicação ainda.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {checkins.map((item) => (
                <div
                  key={item.id}
                  style={{
                    border: '1px solid var(--border)',
                    borderRadius: '8px',
                    padding: '1rem',
                    backgroundColor: 'var(--bg-card)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {item.communityName ? (
                      <Link
                        to={`/communities/${item.communityId}`}
                        style={{
                          fontSize: '0.8rem',
                          fontWeight: 500,
                          backgroundColor: 'var(--bg-secondary)',
                          color: 'var(--text-muted)',
                          padding: '0.15rem 0.5rem',
                          borderRadius: '4px',
                          textDecoration: 'none',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.25rem',
                        }}
                      >
                        <Users size={12} />
                        {item.communityName}
                      </Link>
                    ) : (
                      <span />
                    )}
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                      <Clock size={12} />
                      {new Date(item.createdAt).toLocaleString()}
                    </span>
                  </div>
                  {item.caption && (
                    <p style={{ fontSize: '0.95rem', color: 'var(--text-main)', lineHeight: 1.5, whiteSpace: 'pre-line' }}>
                      {item.caption}
                    </p>
                  )}
                  {item.mediaUrls && item.mediaUrls.length > 0 && (
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '0.6rem' }}>
                      {item.mediaUrls.map((url, idx) => (
                        <img
                          key={idx}
                          src={url}
                          alt={`Foto da publicação ${idx + 1}`}
                          style={{
                            maxHeight: '240px',
                            maxWidth: '100%',
                            borderRadius: '8px',
                            border: '1px solid var(--border)',
                            objectFit: 'cover',
                          }}
                        />
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
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
