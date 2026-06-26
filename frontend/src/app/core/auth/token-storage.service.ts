import { Injectable } from '@angular/core';
import { AuthResponse, CurrentUser } from './auth.models';

const ACCESS_TOKEN_KEY = 'ihm.accessToken';
const REFRESH_TOKEN_KEY = 'ihm.refreshToken';
const USER_KEY = 'ihm.currentUser';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  accessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  refreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  user(): CurrentUser | null {
    const value = localStorage.getItem(USER_KEY);
    return value ? JSON.parse(value) as CurrentUser : null;
  }

  save(auth: AuthResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
  }

  updateUser(user: CurrentUser): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
}
