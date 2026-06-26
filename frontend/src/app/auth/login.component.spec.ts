import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthResponse } from '../core/auth/auth.models';
import { AuthService } from '../core/auth/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let login: AuthService['login'];

  beforeEach(async () => {
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
    login = () => of(authResponse);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
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

    const submitButton = fixture.nativeElement.querySelector('.submit-button') as HTMLButtonElement;
    expect(fixture.nativeElement.textContent).toContain('Invalid username or password');
    expect(submitButton.disabled).toBeFalsy();
    expect(submitButton.textContent).toContain('Sign in');
  });
});
