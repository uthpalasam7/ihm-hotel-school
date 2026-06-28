import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActiveBranchService } from './active-branch.service';
import { TokenStorageService } from './token-storage.service';

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
  const token = tokenStorage.accessToken();
  if (!token) {
    return next(request);
  }

  const headers: Record<string, string> = {
    Authorization: `Bearer ${token}`,
  };
  const activeBranchId = activeBranchService.activeBranchId();
  if (activeBranchId && isBranchScopedRequest(request.url)) {
    headers['X-Active-Branch-Id'] = String(activeBranchId);
  }

  return next(request.clone({
    setHeaders: headers,
  }));
};

function isBranchScopedRequest(url: string): boolean {
  return BRANCH_SCOPED_API_PREFIXES.some((prefix) => url.startsWith(prefix));
}
