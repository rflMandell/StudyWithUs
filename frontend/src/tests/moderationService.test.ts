import { describe, it, expect, vi, beforeEach } from 'vitest';
import { moderationService } from '../services/moderationService';
import { api } from '../services/api';

vi.mock('../services/api');

describe('moderationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('creates a report successfully', async () => {
    const mockReport = {
      id: 'rep-1',
      reporterId: 'u-1',
      targetType: 'COMMENT' as const,
      targetId: 'com-1',
      reason: 'Spam',
      description: 'Suspicious link',
      status: 'PENDING' as const,
      createdAt: '2026-11-12T10:00:00Z',
    };

    vi.mocked(api.post).mockResolvedValue({ data: mockReport });

    const result = await moderationService.createReport({
      targetType: 'COMMENT',
      targetId: 'com-1',
      reason: 'Spam',
      description: 'Suspicious link',
    });

    expect(api.post).toHaveBeenCalledWith('/reports', {
      targetType: 'COMMENT',
      targetId: 'com-1',
      reason: 'Spam',
      description: 'Suspicious link',
    });
    expect(result.id).toBe('rep-1');
    expect(result.status).toBe('PENDING');
  });

  it('lists reports with filters and pagination', async () => {
    const mockResponse = {
      content: [
        {
          id: 'rep-1',
          reporterId: 'u-1',
          targetType: 'COMMENT' as const,
          targetId: 'com-1',
          reason: 'Spam',
          status: 'PENDING' as const,
          createdAt: '2026-11-12T10:00:00Z',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    };

    vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

    const result = await moderationService.listReports('PENDING', 'COMMENT', 0, 10);

    expect(api.get).toHaveBeenCalledWith('/reports', {
      params: { status: 'PENDING', targetType: 'COMMENT', page: 0, size: 10 },
    });
    expect(result.content).toHaveLength(1);
    expect(result.totalElements).toBe(1);
  });

  it('gets a report by id', async () => {
    const mockReport = {
      id: 'rep-1',
      reporterId: 'u-1',
      targetType: 'CHECKIN' as const,
      targetId: 'chk-1',
      reason: 'Inappropriate content',
      status: 'PENDING' as const,
      createdAt: '2026-11-12T10:00:00Z',
    };

    vi.mocked(api.get).mockResolvedValue({ data: mockReport });

    const result = await moderationService.getReport('rep-1');

    expect(api.get).toHaveBeenCalledWith('/reports/rep-1');
    expect(result.id).toBe('rep-1');
    expect(result.targetType).toBe('CHECKIN');
  });

  it('resolves a report with action and notes', async () => {
    const mockResolved = {
      id: 'rep-1',
      reporterId: 'u-1',
      targetType: 'COMMENT' as const,
      targetId: 'com-1',
      reason: 'Spam',
      status: 'RESOLVED' as const,
      reviewedBy: 'mod-1',
      reviewedAt: '2026-11-12T10:15:00Z',
      createdAt: '2026-11-12T10:00:00Z',
    };

    vi.mocked(api.post).mockResolvedValue({ data: mockResolved });

    const result = await moderationService.resolveReport('rep-1', {
      action: 'REMOVE_CONTENT',
      notes: 'Removed abusive comment',
    });

    expect(api.post).toHaveBeenCalledWith('/reports/rep-1/resolve', {
      action: 'REMOVE_CONTENT',
      notes: 'Removed abusive comment',
    });
    expect(result.status).toBe('RESOLVED');
    expect(result.reviewedBy).toBe('mod-1');
  });

  it('dismisses a report', async () => {
    const mockDismissed = {
      id: 'rep-1',
      reporterId: 'u-1',
      targetType: 'COMMENT' as const,
      targetId: 'com-1',
      reason: 'Spam',
      status: 'DISMISSED' as const,
      reviewedBy: 'mod-1',
      reviewedAt: '2026-11-12T10:20:00Z',
      createdAt: '2026-11-12T10:00:00Z',
    };

    vi.mocked(api.post).mockResolvedValue({ data: mockDismissed });

    const result = await moderationService.dismissReport('rep-1', {
      notes: 'No violation found',
    });

    expect(api.post).toHaveBeenCalledWith('/reports/rep-1/dismiss', {
      notes: 'No violation found',
    });
    expect(result.status).toBe('DISMISSED');
  });

  it('lists audit logs with filter and pagination', async () => {
    const mockLogsResponse = {
      content: [
        {
          id: 'log-1',
          actorId: 'mod-1',
          action: 'REPORT_RESOLVED',
          targetType: 'COMMENT',
          targetId: 'com-1',
          metadata: '{"action":"REMOVE_CONTENT"}',
          createdAt: '2026-11-12T10:15:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    };

    vi.mocked(api.get).mockResolvedValue({ data: mockLogsResponse });

    const result = await moderationService.listAuditLogs('COMMENT', 0, 20);

    expect(api.get).toHaveBeenCalledWith('/audit-logs', {
      params: { targetType: 'COMMENT', page: 0, size: 20 },
    });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].action).toBe('REPORT_RESOLVED');
  });
});
