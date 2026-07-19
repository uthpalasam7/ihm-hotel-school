import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthResponse } from '../core/auth/auth.models';
import { AuthService } from '../core/auth/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let login: AuthService['login'];
  let response: AuthResponse;
  let queryParams: Record<string, string>;

  beforeEach(async () => {
    queryParams = {};
    response = {
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
    login = () => of(response);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              get queryParamMap() {
                return convertToParamMap(queryParams);
              },
            },
          },
        },
        {
          provide: AuthService,
          useValue: {
            login: (username: string, password: string) => login(username, password),
          },
        },
      ],
    }).compileComponents();
  });

  it('shows validation messages when submitted empty', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Username is required');
    expect(fixture.nativeElement.textContent).toContain('Password is required');
  });

  it('shows an error and re-enables submit after failed login', async () => {
    login = () => throwError(() => new Error('Unauthorized'));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const [username, password] = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    username.value = 'admin';
    username.dispatchEvent(new Event('input'));
    password.value = 'wrong-password';
    password.dispatchEvent(new Event('input'));

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const submitButton = fixture.nativeElement.querySelector('.auth-submit') as HTMLButtonElement;
    expect(fixture.nativeElement.textContent).toContain('Invalid username or password');
    expect(submitButton.disabled).toBeFalsy();
    expect(submitButton.textContent).toContain('Sign in');
  });

  it('explains when a session has expired', () => {
    queryParams = { reason: 'session-expired' };
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Your session expired. Sign in again to continue.');
  });

  it('shows the password-changed confirmation', () => {
    queryParams = { passwordChanged: 'true' };
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Password changed. Sign in with your new password.');
  });

  it('offers an accessible password visibility control', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const password = fixture.nativeElement.querySelector('input[formControlName="password"]') as HTMLInputElement;
    const toggle = fixture.nativeElement.querySelector('.visibility-toggle') as HTMLButtonElement;
    expect(password.type).toBe('password');
    expect(toggle.getAttribute('aria-label')).toBe('Show password');

    toggle.click();
    fixture.detectChanges();

    expect(password.type).toBe('text');
    expect(toggle.getAttribute('aria-label')).toBe('Hide password');
  });

  it('preserves the forced-password-change redirect after login', () => {
    response = {
      ...response,
      user: { ...response.user, passwordChangeRequired: true },
    };
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl');
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].setValue({ username: 'admin', password: 'Admin@123' });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(navigate).toHaveBeenCalledWith('/change-password');
  });
});
