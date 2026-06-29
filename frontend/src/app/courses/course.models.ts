export type CourseStatus = 'ACTIVE' | 'INACTIVE';

export interface Course {
  id: number;
  name: string;
  shortCode: string;
  description?: string | null;
  status: CourseStatus;
  batchCount: number;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CourseRequest {
  name: string;
  shortCode: string;
  description?: string | null;
  status: CourseStatus;
}
