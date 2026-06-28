import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ActiveBranchService } from './active-branch.service';
import { AuthResponse, CurrentUser } from './auth.models';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly activeBranchService = inject(ActiveBranchService);
  private readonly userSignal = signal<CurrentUser | null>(this.tokenStorage.user());

  readonly currentUser = this.userSignal.asReadonly();
  readonly authenticated = computed(() => {
    const user = this.userSignal();
    const accessToken = this.tokenStorage.accessToken();
    return Boolean(accessToken && user);
  });

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', { username, password }).pipe(
      tap((response) => {
        this.tokenStorage.save(response);
        this.userSignal.set(response.user);
        this.activeBranchService.configure(response.user.branches);
      }),
    );
  }

  logout(): void {
    const refreshToken = this.tokenStorage.refreshToken();
    if (refreshToken) {
      this.http.post('/api/v1/auth/logout', { refreshToken }).subscribe();
    }
    this.tokenStorage.clear();
    this.activeBranchService.clear();
    this.userSignal.set(null);
  }

  me(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>('/api/v1/auth/me').pipe(
      tap((user) => {
        this.tokenStorage.updateUser(user);
        this.userSignal.set(user);
        this.activeBranchService.configure(user.branches);
      }),
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<CurrentUser> {
    return this.http.post<CurrentUser>('/api/v1/auth/change-password', { currentPassword, newPassword }).pipe(
      tap((user) => {
        this.tokenStorage.updateUser(user);
        this.userSignal.set(user);
        this.activeBranchService.configure(user.branches);
      }),
    );
  }

  requiresPasswordChange(): boolean {
    return Boolean(this.userSignal()?.passwordChangeRequired);
  }

  hasAnyRole(roles: string[]): boolean {
    const user = this.userSignal();
    return Boolean(user?.roles.some((role) => roles.includes(role)));
  }
}
