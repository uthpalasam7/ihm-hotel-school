export type ScheduleStatus = 'ACTIVE' | 'INACTIVE';
export interface ScheduleRequest {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  defaultLecturerUserId: number | null;
  classroom: string | null;
  status: ScheduleStatus;
  version?: number;
}
export interface Schedule extends ScheduleRequest {
  id: number;
  batchId: number;
  defaultLecturerName: string | null;
  version: number;
}
export interface GenerationRequest {
  fromDate: string;
  toDate: string;
  excludeDates: string[];
}
export type GenerationOutcome = 'CREATE' | 'ALREADY_GENERATED' | 'EXISTING' | 'CONFLICT';
export interface GenerationEntry {
  scheduleId: number;
  sessionDate: string;
  startTime: string;
  endTime: string;
  lecturerUserId: number | null;
  lecturerName: string | null;
  classroom: string | null;
  outcome: GenerationOutcome;
  existingSessionId: number | null;
  message: string | null;
}
export interface GenerationPreview {
  previewToken: string;
  expiresAt: string;
  createCount: number;
  skipCount: number;
  conflictCount: number;
  sessions: GenerationEntry[];
}
export interface GenerationResult {
  createdCount: number;
  skippedCount: number;
  createdSessionIds: number[];
}
export const WEEKDAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
export const OUTCOME_LABELS: Record<GenerationOutcome, string> = {
  CREATE: 'New session', ALREADY_GENERATED: 'Previously generated', EXISTING: 'Already exists', CONFLICT: 'Needs attention',
};
