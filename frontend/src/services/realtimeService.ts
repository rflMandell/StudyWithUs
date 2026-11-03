import { RealtimeNotification, RealtimeNotificationType } from '../types/realtime';

type NotificationListener = (notification: RealtimeNotification) => void;

class RealtimeService {
  private eventSource: EventSource | null = null;
  private listeners: Set<NotificationListener> = new Set();
  private reconnectTimeout: any = null;
  private isConnecting = false;

  private readonly eventTypes: RealtimeNotificationType[] = [
    'NEW_CHECKIN',
    'RANKING_UPDATED',
    'STREAK_UPDATED',
    'POINTS_AWARDED',
    'MEMBER_JOINED',
  ];

  public connect(): void {
    if (this.eventSource || this.isConnecting) return;

    const token = localStorage.getItem('studywithus_access_token');
    if (!token) return;

    this.isConnecting = true;
    const url = `/api/v1/realtime/stream?token=${encodeURIComponent(token)}`;

    try {
      this.eventSource = new EventSource(url);

      this.eventSource.onopen = () => {
        this.isConnecting = false;
        if (this.reconnectTimeout) {
          clearTimeout(this.reconnectTimeout);
          this.reconnectTimeout = null;
        }
      };

      this.eventTypes.forEach((eventType) => {
        this.eventSource?.addEventListener(eventType, (event: MessageEvent) => {
          try {
            const data: RealtimeNotification = JSON.parse(event.data);
            this.notifyListeners(data);
          } catch (e) {
            console.error('Failed to parse realtime event data:', e);
          }
        });
      });

      this.eventSource.onerror = () => {
        this.disconnect();
        // Exponential backoff reconnect
        if (!this.reconnectTimeout) {
          this.reconnectTimeout = setTimeout(() => {
            this.reconnectTimeout = null;
            this.connect();
          }, 5000);
        }
      };
    } catch (e) {
      this.isConnecting = false;
      console.error('Failed to create EventSource:', e);
    }
  }

  public disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    this.isConnecting = false;
  }

  public subscribe(listener: NotificationListener): () => void {
    this.listeners.add(listener);
    if (!this.eventSource) {
      this.connect();
    }
    return () => {
      this.listeners.delete(listener);
      if (this.listeners.size === 0) {
        this.disconnect();
      }
    };
  }

  private notifyListeners(notification: RealtimeNotification): void {
    this.listeners.forEach((listener) => {
      try {
        listener(notification);
      } catch (err) {
        console.error('Error executing notification listener:', err);
      }
    });
  }
}

export const realtimeService = new RealtimeService();
