import { render, screen, fireEvent, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { RealtimeNotificationToast } from '../components/RealtimeNotificationToast';
import { realtimeService } from '../services/realtimeService';
import { RealtimeNotification } from '../types/realtime';

describe('RealtimeNotificationToast', () => {
  let subscriberCallback: ((notification: RealtimeNotification) => void) | null = null;

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(realtimeService, 'subscribe').mockImplementation((cb) => {
      subscriberCallback = cb;
      return () => {
        subscriberCallback = null;
      };
    });
  });

  it('renders nothing initially when there are no notifications', () => {
    const { container } = render(<RealtimeNotificationToast />);
    expect(container.firstChild).toBeNull();
  });

  it('displays toast when a new check-in realtime notification is received', () => {
    render(<RealtimeNotificationToast />);

    expect(subscriberCallback).toBeDefined();

    act(() => {
      subscriberCallback!({
        id: 'notif-1',
        type: 'NEW_CHECKIN',
        communityId: 'comm-1',
        title: 'Nova atividade na comunidade',
        message: 'Um participante publicou um novo check-in de estudos.',
        data: { checkinId: 'c1' },
        timestamp: new Date().toISOString(),
      });
    });

    expect(screen.getByText('Nova atividade na comunidade')).toBeInTheDocument();
    expect(screen.getByText('Um participante publicou um novo check-in de estudos.')).toBeInTheDocument();
  });

  it('allows dismissing a notification on close button click', () => {
    render(<RealtimeNotificationToast />);

    act(() => {
      subscriberCallback!({
        id: 'notif-2',
        type: 'RANKING_UPDATED',
        communityId: 'comm-1',
        title: 'Ranking atualizado',
        message: 'A classificação da comunidade foi atualizada.',
        data: {},
        timestamp: new Date().toISOString(),
      });
    });

    expect(screen.getByText('Ranking atualizado')).toBeInTheDocument();

    const closeButton = screen.getByRole('button', { name: /fechar notificação/i });
    fireEvent.click(closeButton);

    expect(screen.queryByText('Ranking atualizado')).not.toBeInTheDocument();
  });
});
