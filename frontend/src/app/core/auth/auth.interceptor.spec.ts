import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { ActiveBranchService } from './active-branch.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpTesting: HttpTestingController;
  let activeBranchService: ActiveBranchService;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('ihm.accessToken', 'access-token');
    localStorage.setItem('ihm.accessTokenExpiresAt', '2099-06-26T12:00:00Z');
    localStorage.setItem('ihm.refreshToken', 'refresh-token');
    localStorage.setItem('ihm.refreshTokenExpiresAt', '2099-06-27T12:00:00Z');
    localStorage.setItem('ihm.currentUser', JSON.stringify({
      id: 1,
      username: 'admin',
      fullName: 'Admin User',
      status: 'ACTIVE',
      passwordChangeRequired: false,
      roles: ['ADMIN'],
      branches: [{ id: 2, code: 'IHM-CITY', name: 'IHM City' }],
    }));

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });

    httpTesting = TestBed.inject(HttpTestingController);
    activeBranchService = TestBed.inject(ActiveBranchService);
    activeBranchService.configure([{ id: 2, code: 'IHM-CITY', name: 'IHM City' }]);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('sends active branch context with branch-scoped API requests', () => {
    TestBed.inject(HttpClient).get('/api/v1/users').subscribe();

    const request = httpTesting.expectOne('/api/v1/users');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    expect(request.request.headers.get('X-Active-Branch-Id')).toBe('2');
    request.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('does not send an access token header to auth endpoints', () => {
    TestBed.inject(HttpClient).post('/api/v1/auth/refresh', { refreshToken: 'refresh-token' }).subscribe();

    const request = httpTesting.expectOne('/api/v1/auth/refresh');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(authResponse('new-access-token', 'new-refresh-token'));
  });

  it('keeps the access token header on protected auth endpoints', () => {
    TestBed.inject(HttpClient).get('/api/v1/auth/me').subscribe();

    const request = httpTesting.expectOne('/api/v1/auth/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    request.flush({
      id: 1,
      username: 'admin',
      fullName: 'Admin User',
      status: 'ACTIVE',
      passwordChangeRequired: false,
      roles: ['ADMIN'],
      branches: [{ id: 2, code: 'IHM-CITY', name: 'IHM City' }],
    });
  });

  it('refreshes on protected API 401 and retries the original request', () => {
    const values: unknown[] = [];
    TestBed.inject(HttpClient).get('/api/v1/users').subscribe((value) => values.push(value));

    const original = httpTesting.expectOne('/api/v1/users');
    expect(original.request.headers.get('Authorization')).toBe('Bearer access-token');
    original.flush({}, { status: 401, statusText: 'Unauthorized' });

    const refresh = httpTesting.expectOne('/api/v1/auth/refresh');
    expect(refresh.request.headers.has('Authorization')).toBe(false);
    expect(refresh.request.body).toEqual({ refreshToken: 'refresh-token' });
    refresh.flush(authResponse('new-access-token', 'new-refresh-token'));

    const retry = httpTesting.expectOne('/api/v1/users');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new-access-token');
    retry.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    expect(values.length).toBe(1);
    expect(localStorage.getItem('ihm.refreshToken')).toBe('new-refresh-token');
  });

  it('uses one refresh request when concurrent protected APIs fail with 401', () => {
    TestBed.inject(HttpClient).get('/api/v1/users').subscribe();
    TestBed.inject(HttpClient).get('/api/v1/batches').subscribe();

    const originals = httpTesting.match((request) => request.url === '/api/v1/users' || request.url === '/api/v1/batches');
    expect(originals.length).toBe(2);
    originals.forEach((request) => request.flush({}, { status: 401, statusText: 'Unauthorized' }));

    const refreshes = httpTesting.match('/api/v1/auth/refresh');
    expect(refreshes.length).toBe(1);
    expect(refreshes[0].request.headers.has('Authorization')).toBe(false);
    refreshes[0].flush(authResponse('new-access-token', 'new-refresh-token'));

    const retries = httpTesting.match((request) => request.url === '/api/v1/users' || request.url === '/api/v1/batches');
    expect(retries.length).toBe(2);
    retries.forEach((request) => {
      expect(request.request.headers.get('Authorization')).toBe('Bearer new-access-token');
      request.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    });
  });

  it('clears auth state and redirects to login when refresh fails', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate');
    const errors: unknown[] = [];
    TestBed.inject(HttpClient).get('/api/v1/users').subscribe({ error: (error) => errors.push(error) });

    httpTesting.expectOne('/api/v1/users').flush({}, { status: 401, statusText: 'Unauthorized' });
    httpTesting.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(errors.length).toBe(1);
    expect(localStorage.getItem('ihm.accessToken')).toBeNull();
    expect(localStorage.getItem('ihm.refreshToken')).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { reason: 'session-expired' },
    });
  });
});

function authResponse(accessToken: string, refreshToken: string) {
  return {
    accessToken,
    accessTokenExpiresAt: '2099-06-26T12:00:00Z',
    refreshToken,
    refreshTokenExpiresAt: '2099-06-27T12:00:00Z',
    user: {
      id: 1,
      username: 'admin',
      fullName: 'Admin User',
      status: 'ACTIVE',
      passwordChangeRequired: false,
      roles: ['ADMIN'],
      branches: [{ id: 2, code: 'IHM-CITY', name: 'IHM City' }],
    },
  };
}
