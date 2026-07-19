import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { ActiveBranchService } from './active-branch.service';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';

const AUTH_RETRY = new HttpContextToken<boolean>(() => false);

const BRANCH_SCOPED_API_PREFIXES = [
  '/api/v1/users',
  '/api/v1/batches',
  '/api/v1/students',
  '/api/v1/enrollments',
  '/api/v1/sessions',
  '/api/v1/attendance',
  '/api/v1/payments',
  '/api/v1/reports',
  '/api/v1/dashboard',
  '/api/v1/audit-logs',
];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenStorage = inject(TokenStorageService);
  const activeBranchService = inject(ActiveBranchService);
  const authService = inject(AuthService);
  const router = inject(Router);
  if (isPublicAuthEndpoint(request.url)) {
    return next(request);
  }

  const token = tokenStorage.accessToken();
  const authorizedRequest = token ? withAuthHeaders(request, token, activeBranchService) : request;

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      if (
        !(error instanceof HttpErrorResponse)
        || error.status !== 401
        || request.context.get(AUTH_RETRY)
        || !tokenStorage.refreshToken()
      ) {
        return throwError(() => error);
      }
      return authService.refreshSession().pipe(
        switchMap(() => {
          const refreshedToken = tokenStorage.accessToken();
          if (!refreshedToken) {
            return throwError(() => error);
          }
          const retryRequest = withAuthHeaders(
            request.clone({ context: request.context.set(AUTH_RETRY, true) }),
            refreshedToken,
            activeBranchService,
          );
          return next(retryRequest);
        }),
        catchError((refreshError: unknown) => {
          authService.clearAuthenticationState();
          router.navigate(['/login'], { queryParams: { reason: 'session-expired' } });
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};

function withAuthHeaders(request: Parameters<HttpInterceptorFn>[0], token: string, activeBranchService: ActiveBranchService) {
  const headers: Record<string, string> = {
    Authorization: `Bearer ${token}`,
  };
  const activeBranchId = activeBranchService.activeBranchId();
  if (activeBranchId && isBranchScopedRequest(request.url)) {
    headers['X-Active-Branch-Id'] = String(activeBranchId);
  }
  return request.clone({ setHeaders: headers });
}

function isBranchScopedRequest(url: string): boolean {
  return BRANCH_SCOPED_API_PREFIXES.some((prefix) => url.startsWith(prefix));
}

function isPublicAuthEndpoint(url: string): boolean {
  return url === '/api/v1/auth/login' || url === '/api/v1/auth/refresh' || url === '/api/v1/auth/logout';
}
