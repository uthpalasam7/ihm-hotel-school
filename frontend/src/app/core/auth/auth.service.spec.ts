import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthResponse } from './auth.models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let authService: AuthService;
  let httpTesting: HttpTestingController;

  const authResponse: AuthResponse = {
    accessToken: 'access-token',
    accessTokenExpiresAt: '2099-06-26T12:00:00Z',
    refreshToken: 'refresh-token',
    refreshTokenExpiresAt: '2099-06-27T12:00:00Z',
    user: {
      id: 1,
      username: 'admin',
      fullName: 'Admin User',
      status: 'ACTIVE',
      passwordChangeRequired: false,
      roles: ['SUPER_ADMIN'],
      branches: [{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }],
    },
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    authService = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('updates authenticated state after a successful login from a logged-out page load', () => {
    expect(authService.authenticated()).toBe(false);

    authService.login('admin', 'Admin@123').subscribe();
    const request = httpTesting.expectOne('/api/v1/auth/login');
    request.flush(authResponse);

    expect(authService.authenticated()).toBe(true);
    expect(localStorage.getItem('ihm.accessTokenExpiresAt')).toBe('2099-06-26T12:00:00Z');
    expect(localStorage.getItem('ihm.refreshTokenExpiresAt')).toBe('2099-06-27T12:00:00Z');
  });

  it('restores a stored session without refresh when the access token is still valid', () => {
    localStorage.setItem('ihm.accessToken', 'access-token');
    localStorage.setItem('ihm.accessTokenExpiresAt', '2099-06-26T12:00:00Z');
    localStorage.setItem('ihm.refreshToken', 'refresh-token');
    localStorage.setItem('ihm.refreshTokenExpiresAt', '2099-06-27T12:00:00Z');
    localStorage.setItem('ihm.currentUser', JSON.stringify(authResponse.user));
    authService = TestBed.inject(AuthService);

    let restored: boolean | undefined;
    authService.restoreSession().subscribe((value) => {
      restored = value;
    });

    expect(restored).toBe(true);
    expect(authService.authenticated()).toBe(true);
    httpTesting.expectNone('/api/v1/auth/refresh');
  });

  it('refreshes the stored session on startup when the access token is expired', () => {
    localStorage.setItem('ihm.accessToken', 'expired-access-token');
    localStorage.setItem('ihm.accessTokenExpiresAt', '2000-06-26T12:00:00Z');
    localStorage.setItem('ihm.refreshToken', 'refresh-token');
    localStorage.setItem('ihm.refreshTokenExpiresAt', '2099-06-27T12:00:00Z');
    localStorage.setItem('ihm.currentUser', JSON.stringify(authResponse.user));
    authService = TestBed.inject(AuthService);

    let restored: boolean | undefined;
    authService.restoreSession().subscribe((value) => {
      restored = value;
    });

    const request = httpTesting.expectOne('/api/v1/auth/refresh');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ refreshToken: 'refresh-token' });
    request.flush({ ...authResponse, accessToken: 'new-access-token', refreshToken: 'new-refresh-token' });

    expect(restored).toBe(true);
    expect(localStorage.getItem('ihm.accessToken')).toBe('new-access-token');
    expect(localStorage.getItem('ihm.refreshToken')).toBe('new-refresh-token');
    expect(authService.authenticated()).toBe(true);
  });

  it('clears authentication state when startup refresh fails', () => {
    localStorage.setItem('ihm.accessToken', 'expired-access-token');
    localStorage.setItem('ihm.accessTokenExpiresAt', '2000-06-26T12:00:00Z');
    localStorage.setItem('ihm.refreshToken', 'refresh-token');
    localStorage.setItem('ihm.refreshTokenExpiresAt', '2099-06-27T12:00:00Z');
    localStorage.setItem('ihm.currentUser', JSON.stringify(authResponse.user));
    authService = TestBed.inject(AuthService);

    let restored: boolean | undefined;
    authService.restoreSession().subscribe((value) => {
      restored = value;
    });

    httpTesting.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(restored).toBe(false);
    expect(localStorage.getItem('ihm.accessToken')).toBeNull();
    expect(localStorage.getItem('ihm.refreshToken')).toBeNull();
    expect(authService.authenticated()).toBe(false);
  });

  it('shares one refresh request for concurrent refresh calls', () => {
    localStorage.setItem('ihm.accessToken', 'expired-access-token');
    localStorage.setItem('ihm.accessTokenExpiresAt', '2000-06-26T12:00:00Z');
    localStorage.setItem('ihm.refreshToken', 'refresh-token');
    localStorage.setItem('ihm.refreshTokenExpiresAt', '2099-06-27T12:00:00Z');
    localStorage.setItem('ihm.currentUser', JSON.stringify(authResponse.user));
    authService = TestBed.inject(AuthService);

    const results: string[] = [];
    authService.refreshSession().subscribe((response) => results.push(response.accessToken));
    authService.refreshSession().subscribe((response) => results.push(response.accessToken));

    const requests = httpTesting.match('/api/v1/auth/refresh');
    expect(requests.length).toBe(1);
    requests[0].flush({ ...authResponse, accessToken: 'shared-access-token' });

    expect(results).toEqual(['shared-access-token', 'shared-access-token']);
  });

  it('updates the current user after changing password', () => {
    localStorage.setItem('ihm.accessToken', 'access-token');
    localStorage.setItem('ihm.currentUser', JSON.stringify({ ...authResponse.user, passwordChangeRequired: true }));

    authService.changePassword('Admin@123', 'NewAdmin@123').subscribe();
    const request = httpTesting.expectOne('/api/v1/auth/change-password');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      currentPassword: 'Admin@123',
      newPassword: 'NewAdmin@123',
    });
    request.flush(authResponse.user);

    expect(authService.currentUser()?.passwordChangeRequired).toBe(false);
  });
});
