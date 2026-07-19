import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';
import { ActiveBranchService } from './active-branch.service';
import { AuthResponse, CurrentUser } from './auth.models';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly activeBranchService = inject(ActiveBranchService);
  private readonly userSignal = signal<CurrentUser | null>(this.tokenStorage.user());
  private refreshRequest$: Observable<AuthResponse> | null = null;

  readonly currentUser = this.userSignal.asReadonly();
  readonly authenticated = computed(() => {
    const user = this.userSignal();
    return Boolean(user && this.accessTokenValid());
  });

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', { username, password }).pipe(
      tap((response) => this.applyAuth(response)),
    );
  }

  restoreSession(): Observable<boolean> {
    const user = this.tokenStorage.user();
    if (!user || !this.refreshTokenUsable()) {
      this.clearAuthenticationState();
      return of(false);
    }
    this.userSignal.set(user);
    this.configureBranches(user);
    if (this.accessTokenValid()) {
      return of(true);
    }
    return this.refreshSession().pipe(
      map(() => true),
      catchError(() => {
        this.clearAuthenticationState();
        return of(false);
      }),
    );
  }

  refreshSession(): Observable<AuthResponse> {
    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }
    const refreshToken = this.tokenStorage.refreshToken();
    if (!refreshToken || !this.refreshTokenUsable()) {
      this.clearAuthenticationState();
      return throwError(() => new Error('Refresh token is not available'));
    }
    this.refreshRequest$ = this.http.post<AuthResponse>('/api/v1/auth/refresh', { refreshToken }).pipe(
      tap((response) => this.applyAuth(response)),
      catchError((error) => {
        this.clearAuthenticationState();
        return throwError(() => error);
      }),
      finalize(() => {
        this.refreshRequest$ = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.refreshRequest$;
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

  clearAuthenticationState(): void {
    this.tokenStorage.clear();
    this.activeBranchService.clear();
    this.userSignal.set(null);
  }

  me(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>('/api/v1/auth/me').pipe(
      tap((user) => {
        this.tokenStorage.updateUser(user);
        this.userSignal.set(user);
        this.configureBranches(user);
      }),
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<CurrentUser> {
    return this.http.post<CurrentUser>('/api/v1/auth/change-password', { currentPassword, newPassword }).pipe(
      tap((user) => {
        this.tokenStorage.updateUser(user);
        this.userSignal.set(user);
        this.configureBranches(user);
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

  private applyAuth(response: AuthResponse): void {
    this.tokenStorage.save(response);
    this.userSignal.set(response.user);
    this.configureBranches(response.user);
  }

  private configureBranches(user: CurrentUser): void {
    this.activeBranchService.configure(user.branches, {
      retainStoredSelection: user.roles.includes('SUPER_ADMIN'),
    });
  }

  private accessTokenValid(): boolean {
    return Boolean(this.tokenStorage.accessToken() && this.futureInstant(this.tokenStorage.accessTokenExpiresAt()));
  }

  private refreshTokenUsable(): boolean {
    const refreshToken = this.tokenStorage.refreshToken();
    const expiresAt = this.tokenStorage.refreshTokenExpiresAt();
    return Boolean(refreshToken && (!expiresAt || this.futureInstant(expiresAt)));
  }

  private futureInstant(value: string | null): boolean {
    if (!value) {
      return false;
    }
    const time = Date.parse(value);
    return Number.isFinite(time) && time > Date.now();
  }
}
