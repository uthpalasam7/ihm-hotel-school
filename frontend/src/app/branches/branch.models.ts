export type BranchStatus = 'ACTIVE' | 'INACTIVE';

export interface Branch {
  id: number;
  code: string;
  name: string;
  address?: string | null;
  contactNumber?: string | null;
  status: BranchStatus;
  defaultBranch: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface BranchRequest {
  code: string;
  name: string;
  address?: string | null;
  contactNumber?: string | null;
  status: BranchStatus;
}
