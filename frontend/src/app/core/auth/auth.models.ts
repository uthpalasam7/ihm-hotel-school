export interface BranchSummary {
  id: number;
  code: string;
  name: string;
}

export interface CurrentUser {
  id: number;
  username: string;
  fullName: string;
  status: string;
  passwordChangeRequired: boolean;
  roles: string[];
  branches: BranchSummary[];
}

export interface AuthResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  user: CurrentUser;
}
