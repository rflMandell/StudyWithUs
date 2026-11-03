import React, { useEffect, useState } from 'react';
import { Bell, Flame, Trophy, CheckCircle, X, Sparkles } from 'lucide-react';
import { realtimeService } from '../services/realtimeService';
import { RealtimeNotification } from '../types/realtime';

export const RealtimeNotificationToast: React.FC = () => {
  const [notifications, setNotifications] = useState<RealtimeNotification[]>([]);

  useEffect(() => {
    const unsubscribe = realtimeService.subscribe((notification) => {
      setNotifications((prev) => [notification, ...prev.slice(0, 4)]);

      // Auto-dismiss after 6 seconds
      setTimeout(() => {
        setNotifications((current) => current.filter((n) => n.id !== notification.id));
      }, 6000);
    });

    return () => {
      unsubscribe();
    };
  }, []);

  const dismiss = (id: string) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  if (notifications.length === 0) return null;

  const getIcon = (type: string) => {
    switch (type) {
      case 'NEW_CHECKIN':
        return <Sparkles size={18} color="#2563eb" />;
      case 'RANKING_UPDATED':
        return <Trophy size={18} color="#d97706" />;
      case 'STREAK_UPDATED':
        return <Flame size={18} color="#ea580c" />;
      case 'POINTS_AWARDED':
        return <CheckCircle size={18} color="#16a34a" />;
      default:
        return <Bell size={18} color="#6366f1" />;
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        top: '1.25rem',
        right: '1.25rem',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        gap: '0.75rem',
        maxWidth: '380px',
        width: 'calc(100vw - 2.5rem)',
        pointerEvents: 'none',
      }}
    >
      {notifications.map((notification) => (
        <div
          key={notification.id}
          className="card"
          style={{
            margin: 0,
            padding: '0.875rem 1rem',
            boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
            borderLeft: '4px solid var(--primary)',
            backgroundColor: '#ffffff',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.75rem',
            pointerEvents: 'auto',
            animation: 'fadeIn 0.25s ease-out',
          }}
        >
          <div style={{ marginTop: '0.15rem' }}>{getIcon(notification.type)}</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h4 style={{ margin: 0, fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-main)' }}>
                {notification.title}
              </h4>
              <button
                onClick={() => dismiss(notification.id)}
                aria-label="Fechar notificação"
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--text-muted)',
                  cursor: 'pointer',
                  padding: '0.1rem',
                  display: 'flex',
                  alignItems: 'center',
                }}
              >
                <X size={14} />
              </button>
            </div>
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              {notification.message}
            </p>
          </div>
        </div>
      ))}
    </div>
  );
};
