export type StudentStatus = 'ACTIVE' | 'INACTIVE';

export interface Student {
  id: number;
  fullName: string;
  nic: string;
  contactNumber: string;
  alternativeContactNumber?: string | null;
  email?: string | null;
  address: string;
  dateOfBirth?: string | null;
  gender?: string | null;
  remarks?: string | null;
  status: StudentStatus;
  photoAvailable: boolean;
  photoUrl?: string | null;
  photoThumbnailUrl?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface StudentRequest {
  fullName: string;
  nic: string;
  contactNumber: string;
  alternativeContactNumber?: string | null;
  email?: string | null;
  address: string;
  dateOfBirth?: string | null;
  gender?: string | null;
  remarks?: string | null;
}
