export type SessionStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'RESCHEDULED';

export interface ClassSession {
  id: number;
  batchId: number;
  batchNumber: string;
  courseName: string;
  branchId: number;
  branchName: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  lecturerUserId: number | null;
  lecturerName: string | null;
  topic: string | null;
  classroom: string | null;
  status: SessionStatus;
  cancellationReason: string | null;
  originalSessionId: number | null;
  remarks: string | null;
  attendanceSubmittedAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  sourceScheduleId: number | null;
  generationDate: string | null;
  reschedulingReason: string | null;
}

export interface SessionFilters {
  batchId?: number;
  lecturerId?: number;
  dateFrom?: string;
  dateTo?: string;
  status?: SessionStatus;
  page?: number;
  size?: number;
}

export interface SessionRequest {
  batchId: number;
  sessionDate: string;
  startTime: string;
  endTime: string;
  lecturerUserId: number | null;
  topic: string | null;
  classroom: string | null;
  remarks: string | null;
  version?: number;
}

export interface SessionRescheduleRequest {
  newDate: string;
  newStartTime: string;
  newEndTime: string;
  lecturerUserId: number | null;
  reason: string;
  version: number;
}

export interface SessionRescheduleResponse {
  original: ClassSession;
  replacement: ClassSession;
}
