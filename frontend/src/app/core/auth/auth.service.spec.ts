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
    accessTokenExpiresAt: '2026-06-26T12:00:00Z',
    refreshToken: 'refresh-token',
    refreshTokenExpiresAt: '2026-06-27T12:00:00Z',
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
