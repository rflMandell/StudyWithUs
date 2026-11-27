import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  CheckCircle2,
  Eye,
  FileText,
  Globe,
  Lock,
  Monitor,
  Moon,
  PauseCircle,
  Settings,
  Shield,
  Sun,
  Trash2,
} from 'lucide-react';
import { Navbar } from '../components/Navbar';
import { AlertError } from '../components/AlertError';
import { useAuth } from '../hooks/useAuth';
import { useTheme, ThemeMode } from '../context/ThemeContext';
import { userService } from '../services/userService';
import {
  notificationService,
  NotificationPreferences,
} from '../services/notificationService';

type PolicyTab = 'terms' | 'privacy' | 'cookies' | 'lgpd';

const DEFAULT_PREFS: Omit<NotificationPreferences, 'updatedAt'> = {
  commentsEnabled: true,
  rankingEnabled: true,
  challengeEnabled: true,
  invitationEnabled: true,
  reminderEnabled: true,
  streakEnabled: true,
};

const POLICY_CONTENT: Record<
  PolicyTab,
  { title: string; paragraphs: string[] }
> = {
  terms: {
    title: 'Termos de Uso',
    paragraphs: [
      'Bem-vindo ao StudyWithUs. Ao criar uma conta ou utilizar nossa plataforma, você concorda em cumprir estes Termos de Uso e todas as leis e regulamentos aplicáveis.',
      'Você é responsável por manter a confidencialidade de suas credenciais de acesso e por todas as atividades realizadas em sua conta. É proibido publicar conteúdo ofensivo, fraudulento, discriminatório ou que viole direitos autorais de terceiros.',
      'A plataforma oferece ferramentas de acompanhamento de estudos, comunidades, rankings e desafios. Reservamo-nos o direito de suspender ou encerrar contas que violem as regras da comunidade ou tentem manipular pontuações e rankings.',
      'Você pode desativar temporariamente ou excluir permanentemente sua conta a qualquer momento nesta página de Configurações.',
    ],
  },
  privacy: {
    title: 'Política de Privacidade',
    paragraphs: [
      'Levamos a privacidade dos seus dados a sério. Coletamos apenas as informações necessárias para operar sua conta, personalizar sua experiência acadêmica e calcular sua participação em comunidades e rankings.',
      'Se você optar por uma Conta Pública, seu perfil completo, conquistas, pontuações e atividades nas comunidades ficam visíveis para outros usuários. Em uma Conta Privada, apenas sua foto de perfil, @usuário, nome e biografia são exibidos publicamente.',
      'Não vendemos seus dados pessoais a terceiros. As informações são armazenadas em ambiente seguro com criptografia em trânsito e controle de acesso rigoroso.',
      'Ao excluir sua conta, seus dados pessoais, postagens, comentários e histórico de pontuação são removidos permanentemente da plataforma.',
    ],
  },
  cookies: {
    title: 'Política de Cookies',
    paragraphs: [
      'Utilizamos cookies e armazenamento local (localStorage) estritamente necessários para manter sua sessão autenticada, lembrar suas preferências de tema (Claro, Escuro ou Sistema) e garantir a segurança das requisições.',
      'Não utilizamos cookies de rastreamento publicitário de terceiros. Você pode limpar os dados armazenados nas configurações do seu navegador a qualquer momento, o que encerrará sua sessão ativa.',
    ],
  },
  lgpd: {
    title: 'Diretrizes da Comunidade e LGPD',
    paragraphs: [
      'Em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018 — LGPD), garantimos a você os direitos de confirmação, acesso, retificação, portabilidade e eliminação dos seus dados pessoais.',
      'Nossas comunidades promovem um ambiente colaborativo e respeitoso de estudos. Mantenha o respeito mútuo nos comentários, check-ins e desafios, evitando spam ou assédio.',
      'Para exercer qualquer direito previsto na LGPD ou reportar abusos, utilize os canais de suporte ou as ferramentas de privacidade e exclusão disponíveis nesta página.',
    ],
  },
};

const NOTIFICATION_ITEMS: Array<{
  key: keyof Omit<NotificationPreferences, 'updatedAt'>;
  label: string;
  description: string;
}> = [
  {
    key: 'commentsEnabled',
    label: 'Comentários e interações',
    description: 'Receber avisos quando alguém comentar ou interagir nos seus check-ins.',
  },
  {
    key: 'rankingEnabled',
    label: 'Atualizações de ranking',
    description: 'Notificações sobre mudanças de posição nos rankings das comunidades.',
  },
  {
    key: 'challengeEnabled',
    label: 'Desafios e metas',
    description: 'Alertas sobre novos desafios, prazos e conquistas desbloqueadas.',
  },
  {
    key: 'invitationEnabled',
    label: 'Convites para comunidades',
    description: 'Avisos quando você for convidado para participar de um grupo de estudos.',
  },
  {
    key: 'reminderEnabled',
    label: 'Lembretes de estudo',
    description: 'Lembretes periódicos para manter sua rotina de estudos em dia.',
  },
  {
    key: 'streakEnabled',
    label: 'Alertas de sequência (streak)',
    description: 'Notificações para não perder sua sequência diária de check-ins.',
  },
];

export const SettingsPage: React.FC = () => {
  const { user, updateUser, logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const navigate = useNavigate();

  // Password state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);

  // Privacy state
  const [privacyLoading, setPrivacyLoading] = useState(false);
  const [privacyError, setPrivacyError] = useState<string | null>(null);
  const [privacySuccess, setPrivacySuccess] = useState<string | null>(null);

  // Deactivate / Delete state
  const [showDeactivateConfirm, setShowDeactivateConfirm] = useState(false);
  const [deactivateLoading, setDeactivateLoading] = useState(false);
  const [deactivateError, setDeactivateError] = useState<string | null>(null);

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteConfirmText, setDeleteConfirmText] = useState('');
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // Notifications state
  const [prefs, setPrefs] = useState<Omit<NotificationPreferences, 'updatedAt'>>(DEFAULT_PREFS);
  const [prefsLoading, setPrefsLoading] = useState(true);
  const [prefsSaving, setPrefsSaving] = useState(false);
  const [prefsError, setPrefsError] = useState<string | null>(null);
  const [prefsSuccess, setPrefsSuccess] = useState<string | null>(null);

  // Policy tab state
  const [activePolicyTab, setActivePolicyTab] = useState<PolicyTab>('terms');

  useEffect(() => {
    let active = true;
    const loadPrefs = async () => {
      setPrefsLoading(true);
      try {
        const data = await notificationService.getPreferences();
        if (!active) return;
        setPrefs({
          commentsEnabled: data.commentsEnabled,
          rankingEnabled: data.rankingEnabled,
          challengeEnabled: data.challengeEnabled,
          invitationEnabled: data.invitationEnabled,
          reminderEnabled: data.reminderEnabled,
          streakEnabled: data.streakEnabled,
        });
      } catch {
        if (!active) return;
        setPrefsError('Não foi possível carregar as preferências de notificação.');
      } finally {
        if (active) setPrefsLoading(false);
      }
    };
    void loadPrefs();
    return () => {
      active = false;
    };
  }, []);

  const extractApiMessage = (err: any, fallback: string): string => {
    return err?.response?.data?.message || err?.response?.data?.code || err?.message || fallback;
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(null);

    if (newPassword.length < 8) {
      setPasswordError('A nova senha deve ter pelo menos 8 caracteres.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordError('A nova senha e a confirmação não coincidem.');
      return;
    }

    setPasswordLoading(true);
    try {
      await userService.changePassword({
        currentPassword,
        newPassword,
        confirmPassword,
      });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPasswordSuccess('Senha alterada com sucesso!');
    } catch (err: any) {
      setPasswordError(extractApiMessage(err, 'Não foi possível alterar a senha.'));
    } finally {
      setPasswordLoading(false);
    }
  };

  const handlePrivacyChange = async (isPrivate: boolean) => {
    if (user?.isPrivate === isPrivate) return;
    setPrivacyLoading(true);
    setPrivacyError(null);
    setPrivacySuccess(null);
    try {
      const updated = await userService.updatePrivacy(isPrivate);
      updateUser(updated);
      setPrivacySuccess(
        isPrivate
          ? 'Sua conta agora é Privada.'
          : 'Sua conta agora é Pública.'
      );
    } catch (err: any) {
      if (err?.response?.status === 429) {
        setPrivacyError(
          extractApiMessage(
            err,
            'Limite atingido: a privacidade só pode ser alterada 1 vez a cada 10 minutos.'
          )
        );
      } else {
        setPrivacyError(extractApiMessage(err, 'Erro ao atualizar privacidade da conta.'));
      }
    } finally {
      setPrivacyLoading(false);
    }
  };

  const handleDeactivateAccount = async () => {
    setDeactivateLoading(true);
    setDeactivateError(null);
    try {
      await userService.deactivateAccount();
      await logout();
      navigate('/login');
    } catch (err: any) {
      setDeactivateError(extractApiMessage(err, 'Erro ao desativar conta.'));
      setDeactivateLoading(false);
    }
  };

  const handleDeleteAccount = async () => {
    if (deleteConfirmText.trim().toUpperCase() !== 'EXCLUIR') {
      setDeleteError('Digite EXCLUIR para confirmar a exclusão permanente.');
      return;
    }
    setDeleteLoading(true);
    setDeleteError(null);
    try {
      await userService.deleteAccount();
      await logout();
      navigate('/login');
    } catch (err: any) {
      setDeleteError(extractApiMessage(err, 'Erro ao excluir conta.'));
      setDeleteLoading(false);
    }
  };

  const handleTogglePref = (key: keyof Omit<NotificationPreferences, 'updatedAt'>) => {
    setPrefs((prev) => ({ ...prev, [key]: !prev[key] }));
    setPrefsSuccess(null);
  };

  const handleSavePreferences = async () => {
    setPrefsSaving(true);
    setPrefsError(null);
    setPrefsSuccess(null);
    try {
      const saved = await notificationService.updatePreferences(prefs);
      setPrefs({
        commentsEnabled: saved.commentsEnabled,
        rankingEnabled: saved.rankingEnabled,
        challengeEnabled: saved.challengeEnabled,
        invitationEnabled: saved.invitationEnabled,
        reminderEnabled: saved.reminderEnabled,
        streakEnabled: saved.streakEnabled,
      });
      setPrefsSuccess('Preferências de notificação salvas com sucesso!');
    } catch (err: any) {
      setPrefsError(extractApiMessage(err, 'Erro ao salvar preferências de notificação.'));
    } finally {
      setPrefsSaving(false);
    }
  };

  const themeOptions: Array<{
    mode: ThemeMode;
    label: string;
    description: string;
    icon: React.ReactNode;
  }> = [
    {
      mode: 'light',
      label: 'Claro',
      description: 'Visual claro padrão para ambientes iluminados.',
      icon: <Sun size={20} />,
    },
    {
      mode: 'dark',
      label: 'Escuro',
      description: 'Cores escuras para reduzir o cansaço visual.',
      icon: <Moon size={20} />,
    },
    {
      mode: 'system',
      label: 'Sistema operacional',
      description: 'Acompanha automaticamente a preferência do seu dispositivo.',
      icon: <Monitor size={20} />,
    },
  ];

  return (
    <div>
      <Navbar />
      <main className="container" style={{ maxWidth: '900px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '2rem' }}>
          <Settings size={28} color="var(--primary)" />
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 700, letterSpacing: '-0.025em' }}>
              Configurações
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>
              Gerencie sua segurança, privacidade, aparência e preferências da plataforma.
            </p>
          </div>
        </div>

        {/* SEÇÃO 1: Configurações do Perfil */}
        <section aria-labelledby="section-profile-settings" style={{ marginBottom: '2.5rem' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              marginBottom: '1rem',
              paddingBottom: '0.5rem',
              borderBottom: '2px solid var(--border)',
            }}
          >
            <Shield size={20} color="var(--primary)" />
            <h2 id="section-profile-settings" style={{ fontSize: '1.25rem', fontWeight: 700 }}>
              Configurações do Perfil
            </h2>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {/* 1. Alterar Senha */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <Lock size={18} color="var(--primary)" />
                <h3 style={{ margin: 0 }}>Alterar Senha</h3>
              </div>

              {passwordError && <AlertError message={passwordError} />}
              {passwordSuccess && (
                <div
                  role="status"
                  style={{
                    backgroundColor: 'var(--success-light)',
                    color: 'var(--success)',
                    border: '1px solid var(--success)',
                    padding: '0.75rem 1rem',
                    borderRadius: '6px',
                    marginBottom: '1rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    fontSize: '0.9rem',
                  }}
                >
                  <CheckCircle2 size={16} />
                  <span>{passwordSuccess}</span>
                </div>
              )}

              <form onSubmit={handlePasswordSubmit}>
                <div className="form-group">
                  <label htmlFor="current-password">Senha atual</label>
                  <input
                    id="current-password"
                    type="password"
                    className="form-control"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="new-password">Nova senha</label>
                  <input
                    id="new-password"
                    type="password"
                    className="form-control"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    minLength={8}
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="confirm-password">Confirmar nova senha</label>
                  <input
                    id="confirm-password"
                    type="password"
                    className="form-control"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    minLength={8}
                    required
                  />
                </div>
                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{ width: 'auto' }}
                  disabled={passwordLoading}
                >
                  {passwordLoading ? 'Alterando...' : 'Atualizar Senha'}
                </button>
              </form>
            </div>

            {/* 2. Privacidade da Conta */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <Eye size={18} color="var(--primary)" />
                <h3 style={{ margin: 0 }}>Privacidade da Conta</h3>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '0.5rem' }}>
                Escolha quem pode visualizar suas informações e atividades na plataforma:
              </p>
              <ul
                style={{
                  color: 'var(--text-muted)',
                  fontSize: '0.875rem',
                  paddingLeft: '1.25rem',
                  marginBottom: '0.75rem',
                }}
              >
                <li>
                  <strong>Conta Pública:</strong> perfil inteiro e atividades visíveis para qualquer pessoa.
                </li>
                <li>
                  <strong>Conta Privada:</strong> apenas foto, @, nome e bio visíveis para outros usuários.
                </li>
              </ul>
              <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                Alteração permitida 1 vez a cada 10 minutos.
              </p>

              {privacyError && <AlertError message={privacyError} />}
              {privacySuccess && (
                <div
                  role="status"
                  style={{
                    backgroundColor: 'var(--success-light)',
                    color: 'var(--success)',
                    border: '1px solid var(--success)',
                    padding: '0.75rem 1rem',
                    borderRadius: '6px',
                    marginBottom: '1rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    fontSize: '0.9rem',
                  }}
                >
                  <CheckCircle2 size={16} />
                  <span>{privacySuccess}</span>
                </div>
              )}

              <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                <button
                  type="button"
                  onClick={() => handlePrivacyChange(false)}
                  disabled={privacyLoading}
                  className={!user?.isPrivate ? 'btn btn-primary' : 'btn btn-secondary'}
                  style={{ width: 'auto', gap: '0.4rem' }}
                  aria-pressed={!user?.isPrivate}
                >
                  <Globe size={16} />
                  <span>Conta Pública</span>
                </button>
                <button
                  type="button"
                  onClick={() => handlePrivacyChange(true)}
                  disabled={privacyLoading}
                  className={user?.isPrivate ? 'btn btn-primary' : 'btn btn-secondary'}
                  style={{ width: 'auto', gap: '0.4rem' }}
                  aria-pressed={!!user?.isPrivate}
                >
                  <Lock size={16} />
                  <span>Conta Privada</span>
                </button>
              </div>
            </div>

            {/* 3. Desativar Conta */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <PauseCircle size={18} color="var(--primary)" />
                <h3 style={{ margin: 0 }}>Desativar Conta</h3>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1rem' }}>
                Ao desativar sua conta, seus posts, perfil, colocação nos rankings e dados serão
                arquivados (mantidos salvos no sistema), você será deslogado e sua conta será
                reativada automaticamente ao fazer login novamente.
              </p>

              {deactivateError && <AlertError message={deactivateError} />}

              {!showDeactivateConfirm ? (
                <button
                  type="button"
                  className="btn btn-secondary"
                  style={{ width: 'auto', gap: '0.4rem' }}
                  onClick={() => setShowDeactivateConfirm(true)}
                >
                  <PauseCircle size={16} />
                  <span>Desativar Conta</span>
                </button>
              ) : (
                <div
                  style={{
                    padding: '1rem',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    backgroundColor: 'var(--bg-main)',
                  }}
                >
                  <p style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: '0.75rem' }}>
                    Tem certeza que deseja desativar sua conta?
                  </p>
                  <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                    <button
                      type="button"
                      className="btn btn-primary"
                      style={{ width: 'auto' }}
                      onClick={handleDeactivateAccount}
                      disabled={deactivateLoading}
                    >
                      {deactivateLoading ? 'Desativando...' : 'Confirmar Desativação'}
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary"
                      style={{ width: 'auto' }}
                      onClick={() => setShowDeactivateConfirm(false)}
                      disabled={deactivateLoading}
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* 4. Excluir Conta */}
            <div
              className="card"
              style={{
                borderColor: 'var(--danger-border)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <Trash2 size={18} color="var(--danger)" />
                <h3 style={{ margin: 0, color: 'var(--danger)' }}>Excluir Conta</h3>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1rem' }}>
                Zona de perigo: tudo — incluindo sua colocação nos rankings, posts, fotos,
                comentários, perfil e usuário — será excluído permanentemente e não arquivado. Esta
                ação não pode ser desfeita.
              </p>

              {deleteError && <AlertError message={deleteError} />}

              {!showDeleteConfirm ? (
                <button
                  type="button"
                  className="btn"
                  style={{
                    width: 'auto',
                    gap: '0.4rem',
                    backgroundColor: 'var(--danger)',
                    color: '#ffffff',
                  }}
                  onClick={() => setShowDeleteConfirm(true)}
                >
                  <Trash2 size={16} />
                  <span>Excluir Conta Permanentemente</span>
                </button>
              ) : (
                <div
                  style={{
                    padding: '1rem',
                    borderRadius: '8px',
                    border: '1px solid var(--danger-border)',
                    backgroundColor: 'var(--danger-light)',
                  }}
                >
                  <p style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: '0.5rem', color: 'var(--danger-text)' }}>
                    Digite EXCLUIR para confirmar a exclusão permanente da sua conta:
                  </p>
                  <input
                    type="text"
                    className="form-control"
                    aria-label="Confirmar exclusão digitando EXCLUIR"
                    placeholder="EXCLUIR"
                    value={deleteConfirmText}
                    onChange={(e) => setDeleteConfirmText(e.target.value)}
                    style={{ marginBottom: '0.75rem', maxWidth: '280px' }}
                  />
                  <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                    <button
                      type="button"
                      className="btn"
                      style={{
                        width: 'auto',
                        backgroundColor: 'var(--danger)',
                        color: '#ffffff',
                      }}
                      onClick={handleDeleteAccount}
                      disabled={deleteLoading}
                    >
                      {deleteLoading ? 'Excluindo...' : 'Confirmar Exclusão'}
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary"
                      style={{ width: 'auto' }}
                      onClick={() => {
                        setShowDeleteConfirm(false);
                        setDeleteConfirmText('');
                        setDeleteError(null);
                      }}
                      disabled={deleteLoading}
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </section>

        {/* SEÇÃO 2: Configurações Gerais */}
        <section aria-labelledby="section-general-settings" style={{ marginBottom: '2rem' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              marginBottom: '1rem',
              paddingBottom: '0.5rem',
              borderBottom: '2px solid var(--border)',
            }}
          >
            <Settings size={20} color="var(--primary)" />
            <h2 id="section-general-settings" style={{ fontSize: '1.25rem', fontWeight: 700 }}>
              Configurações Gerais
            </h2>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {/* 1. Aparência */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <Sun size={18} color="var(--primary)" />
                <h3 style={{ margin: 0 }}>Aparência</h3>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1rem' }}>
                Escolha entre o modo claro, escuro ou acompanhe o tema do seu sistema operacional.
              </p>

              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '0.75rem',
                }}
              >
                {themeOptions.map((option) => {
                  const isActive = theme === option.mode;
                  return (
                    <button
                      key={option.mode}
                      type="button"
                      onClick={() => setTheme(option.mode)}
                      aria-pressed={isActive}
                      style={{
                        textAlign: 'left',
                        padding: '1rem',
                        borderRadius: '8px',
                        border: isActive ? '2px solid var(--primary)' : '1px solid var(--border)',
                        backgroundColor: isActive ? 'var(--primary-light)' : 'var(--bg-card)',
                        color: 'var(--text-main)',
                        cursor: 'pointer',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '0.4rem',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600 }}>
                        {option.icon}
                        <span>{option.label}</span>
                      </div>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {option.description}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 2. Notificações */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <Bell size={18} color="var(--primary)" />
                <h3 style={{ margin: 0 }}>Notificações</h3>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1rem' }}>
                Escolha quais tipos de notificações você deseja receber.
              </p>

              {prefsError && <AlertError message={prefsError} />}
              {prefsSuccess && (
                <div
                  role="status"
                  style={{
                    backgroundColor: 'var(--success-light)',
                    color: 'var(--success)',
                    border: '1px solid var(--success)',
                    padding: '0.75rem 1rem',
                    borderRadius: '6px',
                    marginBottom: '1rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    fontSize: '0.9rem',
                  }}
                >
                  <CheckCircle2 size={16} />
                  <span>{prefsSuccess}</span>
                </div>
              )}

              {prefsLoading ? (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                  Carregando preferências...
                </p>
              ) : (
                <>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', marginBottom: '1.25rem' }}>
                    {NOTIFICATION_ITEMS.map((item) => (
                      <label
                        key={item.key}
                        style={{
                          display: 'flex',
                          alignItems: 'flex-start',
                          gap: '0.75rem',
                          cursor: 'pointer',
                          padding: '0.6rem',
                          borderRadius: '6px',
                          border: '1px solid var(--border)',
                        }}
                      >
                        <input
                          type="checkbox"
                          checked={prefs[item.key]}
                          onChange={() => handleTogglePref(item.key)}
                          style={{ marginTop: '0.2rem' }}
                        />
                        <div>
                          <div style={{ fontWeight: 600, fontSize: '0.925rem' }}>{item.label}</div>
                          <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                            {item.description}
                          </div>
                        </div>
                      </label>
                    ))}
                  </div>
                  <button
                    type="button"
                    className="btn btn-primary"
                    style={{ width: 'auto' }}
                    onClick={handleSavePreferences}
                    disabled={prefsSaving}
                  >
                    {prefsSaving ? 'Salvando...' : 'Salvar Preferências'}
                  </button>
                </>
              )}
            </div>

            {/* 3. Termos, Privacidade e Políticas */}
            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <FileText size={18} color="var(--primary)" />
                <h3 style={{ margin: 0 }}>Termos, Privacidade e Políticas</h3>
              </div>

              <div
                role="tablist"
                aria-label="Documentos legais"
                style={{
                  display: 'flex',
                  gap: '0.5rem',
                  flexWrap: 'wrap',
                  marginBottom: '1rem',
                  borderBottom: '1px solid var(--border)',
                  paddingBottom: '0.75rem',
                }}
              >
                {(
                  [
                    ['terms', 'Termos de Uso'],
                    ['privacy', 'Política de Privacidade'],
                    ['cookies', 'Política de Cookies'],
                    ['lgpd', 'Diretrizes da Comunidade e LGPD'],
                  ] as Array<[PolicyTab, string]>
                ).map(([tabKey, tabLabel]) => {
                  const selected = activePolicyTab === tabKey;
                  return (
                    <button
                      key={tabKey}
                      type="button"
                      role="tab"
                      aria-selected={selected}
                      onClick={() => setActivePolicyTab(tabKey)}
                      className={selected ? 'btn btn-primary' : 'btn btn-secondary'}
                      style={{ width: 'auto', padding: '0.45rem 0.9rem', fontSize: '0.85rem' }}
                    >
                      {tabLabel}
                    </button>
                  );
                })}
              </div>

              <div role="tabpanel" style={{ fontSize: '0.925rem', color: 'var(--text-main)' }}>
                <h4 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.6rem' }}>
                  {POLICY_CONTENT[activePolicyTab].title}
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem', color: 'var(--text-muted)' }}>
                  {POLICY_CONTENT[activePolicyTab].paragraphs.map((paragraph, index) => (
                    <p key={index}>{paragraph}</p>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
};
