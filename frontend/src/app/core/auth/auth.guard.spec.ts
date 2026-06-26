import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard, guestGuard } from './auth.guard';

describe('auth route guards', () => {
  let router: Router;
  let authenticated = false;
  let requiresPasswordChange = false;

  beforeEach(() => {
    authenticated = false;
    requiresPasswordChange = false;

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            authenticated: () => authenticated,
            requiresPasswordChange: () => requiresPasswordChange,
          },
        },
      ],
    });

    router = TestBed.inject(Router);
  });

  function state(url: string): RouterStateSnapshot {
    return { url } as RouterStateSnapshot;
  }

  it('redirects logged-out users from protected routes to login', () => {
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, state('/')));

    expect(router.serializeUrl(result as never)).toBe('/login');
  });

  it('redirects password-change-required users from dashboard to change password', () => {
    authenticated = true;
    requiresPasswordChange = true;

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, state('/')));

    expect(router.serializeUrl(result as never)).toBe('/change-password');
  });

  it('allows password-change-required users to open change password', () => {
    authenticated = true;
    requiresPasswordChange = true;

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, state('/change-password')));

    expect(result).toBe(true);
  });

  it('allows logged-out users to open login', () => {
    const result = TestBed.runInInjectionContext(() => guestGuard({} as never, state('/login')));

    expect(result).toBe(true);
  });

  it('redirects authenticated users away from login to dashboard', () => {
    authenticated = true;

    const result = TestBed.runInInjectionContext(() => guestGuard({} as never, state('/login')));

    expect(router.serializeUrl(result as never)).toBe('/');
  });

  it('redirects password-change-required users away from login to change password', () => {
    authenticated = true;
    requiresPasswordChange = true;

    const result = TestBed.runInInjectionContext(() => guestGuard({} as never, state('/login')));

    expect(router.serializeUrl(result as never)).toBe('/change-password');
  });
});
