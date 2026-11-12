export type ReportTargetType = 'CHECKIN' | 'COMMENT' | 'USER' | 'COMMUNITY';

export type ReportStatus = 'PENDING' | 'RESOLVED' | 'DISMISSED';

export type ModerationAction = 'DISMISS' | 'REMOVE_CONTENT' | 'WARN_USER' | 'SUSPEND_USER';

export interface Report {
  id: string;
  reporterId: string;
  targetType: ReportTargetType;
  targetId: string;
  reason: string;
  description?: string | null;
  status: ReportStatus;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  createdAt: string;
}

export interface CreateReportData {
  targetType: ReportTargetType;
  targetId: string;
  reason: string;
  description?: string;
}

export interface ResolveReportData {
  action: ModerationAction;
  notes?: string;
}

export interface DismissReportData {
  notes?: string;
}

export interface AuditLog {
  id: string;
  actorId: string;
  action: string;
  targetType: string;
  targetId: string;
  metadata?: string | null;
  createdAt: string;
}

export interface PagedReportsResponse {
  content: Report[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface PagedAuditLogsResponse {
  content: AuditLog[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
