import { BranchSummary } from '../core/auth/auth.models';

export type BatchStatus = 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type ScheduleMode = 'REGULAR' | 'MANUAL';
export type FeePlanStatus = 'ACTIVE' | 'INACTIVE';
export type BatchLecturerStatus = 'ACTIVE' | 'INACTIVE';

export interface BatchCourseSummary {
  id: number;
  name: string;
  shortCode: string;
}

export interface Batch {
  id: number;
  course: BatchCourseSummary;
  branch: BranchSummary;
  batchNumber: string;
  startDate: string;
  endDate: string;
  durationMonths: number;
  scheduleMode: ScheduleMode;
  status: BatchStatus;
  remarks?: string | null;
  registrationSequence: number;
  lecturerCount: number;
  studentCount: number;
  feePlanConfigured: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface BatchRequest {
  courseId: number;
  branchId: number;
  batchNumber: string;
  startDate: string;
  endDate: string;
  durationMonths: number;
  scheduleMode: ScheduleMode;
  status: BatchStatus;
  remarks?: string | null;
}

export interface FeePlanRequest {
  registrationFee: number;
  courseFee: number;
  examinationFee: number;
  durationMonths: number;
  monthlyDueDay: number;
  examinationDueDate: string;
  currencyCode: string;
  status: FeePlanStatus;
}

export interface FeePlan extends FeePlanRequest {
  id: number;
  batchId: number;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface PreviewCharge {
  type: string;
  installmentNumber?: number | null;
  description: string;
  dueDate: string;
  amount: number;
}

export interface InstallmentPreview {
  currencyCode: string;
  totalAmount: number;
  charges: PreviewCharge[];
}

export interface BatchLecturer {
  id: number;
  batchId: number;
  lecturerUserId: number;
  lecturerFullName: string;
  lecturerUsername: string;
  assignmentStartDate: string;
  assignmentEndDate?: string | null;
  status: BatchLecturerStatus;
  createdAt: string;
  updatedAt: string;
}

export interface BatchLecturerRequest {
  lecturerUserId: number;
  assignmentStartDate: string;
  assignmentEndDate?: string | null;
  status: BatchLecturerStatus;
}

export interface BatchLecturerSyncRequest {
  lecturerUserIds: number[];
  assignmentStartDate: string;
  assignmentEndDate?: string | null;
}
