import { api } from './api';
import {
  AuditLog,
  CreateReportData,
  DismissReportData,
  PagedAuditLogsResponse,
  PagedReportsResponse,
  Report,
  ReportStatus,
  ReportTargetType,
  ResolveReportData,
} from '../types/moderation';

export const moderationService = {
  async createReport(data: CreateReportData): Promise<Report> {
    const response = await api.post<Report>('/reports', data);
    return response.data;
  },

  async listReports(
    status?: ReportStatus,
    targetType?: ReportTargetType,
    page = 0,
    size = 20
  ): Promise<PagedReportsResponse> {
    const response = await api.get<PagedReportsResponse>('/reports', {
      params: { status, targetType, page, size },
    });
    return response.data;
  },

  async getReport(id: string): Promise<Report> {
    const response = await api.get<Report>(`/reports/${id}`);
    return response.data;
  },

  async resolveReport(id: string, data: ResolveReportData): Promise<Report> {
    const response = await api.post<Report>(`/reports/${id}/resolve`, data);
    return response.data;
  },

  async dismissReport(id: string, data?: DismissReportData): Promise<Report> {
    const response = await api.post<Report>(`/reports/${id}/dismiss`, data || {});
    return response.data;
  },

  async listAuditLogs(
    targetType?: string,
    page = 0,
    size = 20
  ): Promise<PagedAuditLogsResponse> {
    const response = await api.get<PagedAuditLogsResponse>('/audit-logs', {
      params: { targetType, page, size },
    });
    return response.data;
  },
};
