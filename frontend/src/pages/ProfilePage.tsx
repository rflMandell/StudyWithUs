import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  Award,
  CheckCircle2,
  Edit3,
  Flame,
  School,
  Sparkles,
  Users,
  X,
  Save,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { Navbar } from '../components/Navbar';
import { User, UpdateUserData, UserBadge } from '../types/auth';
import { userService } from '../services/userService';
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
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form state
  const [formData, setFormData] = useState<UpdateUserData>({
    name: '',
    bio: '',
    institution: '',
    course: '',
    studyGoal: '',
    avatarUrl: '',
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
        avatarUrl: data.avatarUrl || '',
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

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
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
        avatarUrl: formData.avatarUrl?.trim() || undefined,
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
              <div
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
                  border: '3px solid #e2e8f0',
                  flexShrink: 0,
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

              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
                  <h1 style={{ fontSize: '1.75rem', fontWeight: 700, letterSpacing: '-0.025em' }}>
                    {profile.name}
                  </h1>
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
                </div>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', marginTop: '0.15rem' }}>
                  @{profile.username}
                </p>
                {profile.bio && (
                  <p style={{ marginTop: '0.5rem', fontSize: '0.95rem', color: '#334155', maxWidth: '600px' }}>
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
                    URL do Avatar (Foto de Perfil)
                  </label>
                  <input
                    type="url"
                    name="avatarUrl"
                    value={formData.avatarUrl}
                    onChange={handleInputChange}
                    placeholder="https://exemplo.com/minha-foto.jpg"
                    className="input"
                  />
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
                      border: '1px solid var(--border)',
                      borderRadius: '8px',
                      padding: '0.75rem',
                      textAlign: 'center',
                      backgroundColor: '#fffbeb',
                    }}
                  >
                    <div style={{ fontSize: '1.75rem', marginBottom: '0.25rem' }}>🏆</div>
                    <p style={{ fontWeight: 600, fontSize: '0.85rem', color: '#92400e' }}>{badge.name}</p>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                      {badge.description}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
};
