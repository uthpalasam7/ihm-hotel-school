import { BranchSummary } from '../core/auth/auth.models';

export type UserStatus = 'ACTIVE' | 'DISABLED' | 'PASSWORD_CHANGE_REQUIRED';

export interface Role {
  id: number;
  code: string;
  name: string;
}

export interface UserAccount {
  id: number;
  username: string;
  email?: string | null;
  fullName: string;
  contactNumber?: string | null;
  status: UserStatus;
  roles: Role[];
  branches: BranchSummary[];
  lastLoginAt?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  temporaryPassword?: string | null;
}

export interface UserRequest {
  username: string;
  email?: string | null;
  fullName: string;
  contactNumber?: string | null;
  status: UserStatus;
  roleCodes: string[];
  branchIds: number[];
  temporaryPassword?: string | null;
}

export interface UserProfileRequest {
  username: string;
  email?: string | null;
  fullName: string;
  contactNumber?: string | null;
}
