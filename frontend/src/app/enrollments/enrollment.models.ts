export type EnrollmentStatus = 'ACTIVE' | 'COMPLETED' | 'WITHDRAWN' | 'SUSPENDED' | 'CANCELLED';

export interface Enrollment {
  id: number;
  studentId: number;
  studentName: string;
  batchId: number;
  batchNumber: string;
  courseName: string;
  branchId: number;
  branchName: string;
  registrationNumber: string;
  enrollmentDate: string;
  status: EnrollmentStatus;
  remarks: string | null;
  createdAt: string;
  version: number;
}
export interface EnrollmentRequest {
  studentId: number;
  batchId: number;
  enrollmentDate: string;
  remarks: string;
  expectedBatchVersion?: number;
  expectedFeePlanVersion?: number;
}
export interface EnrollmentPreview {
  currencyCode: string;
  totalAmount: number;
  batchVersion: number;
  feePlanVersion: number;
  charges: { type: string; installmentNumber: number | null; description: string; dueDate: string; amount: number }[];
}
export interface StudentCharge {
  id: number;
  type: string;
  installmentNumber: number | null;
  description: string;
  dueDate: string;
  originalAmount: number;
  discountAmount: number;
  waiverAmount: number;
  finalPayableAmount: number;
  currencyCode: string;
  status: string;
}
export interface BatchStudent {
  studentId: number;
  studentName: string;
  registrationNumber: string;
  enrollmentDate: string;
  status: EnrollmentStatus;
}
