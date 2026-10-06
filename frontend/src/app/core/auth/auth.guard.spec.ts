import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard, guestGuard, roleGuard } from './auth.guard';
import { routes } from '../../app.routes';

describe('auth route guards', () => {
  let router: Router;
  let authenticated = false;
  let requiresPasswordChange = false;
  let currentRoles: string[] = [];

  beforeEach(() => {
    authenticated = false;
    requiresPasswordChange = false;
    currentRoles = [];

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            authenticated: () => authenticated,
            requiresPasswordChange: () => requiresPasswordChange,
            hasAnyRole: (roles: string[]) => roles.some((role) => currentRoles.includes(role)),
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

    expect(router.serializeUrl(result as never)).toBe('/login?reason=sign-in-required');
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

  it('redirects users without a required role to the access-denied page', () => {
    currentRoles = ['LECTURER'];

    const result = TestBed.runInInjectionContext(() => roleGuard(['SUPER_ADMIN'])({} as never, state('/branches')));

    expect(router.serializeUrl(result as never)).toBe('/forbidden');
  });

  it('allows users with a required role to continue', () => {
    currentRoles = ['ADMIN'];

    const result = TestBed.runInInjectionContext(() => roleGuard(['SUPER_ADMIN', 'ADMIN'])({} as never, state('/users')));

    expect(result).toBe(true);
  });

  it('opens class sessions to lecturers while keeping weekly schedule admin-only', () => {
    authenticated = true;
    currentRoles = ['LECTURER'];
    const children = routes.find(route => route.path === '')?.children ?? [];
    const sessions = children.find(route => route.path === 'sessions');
    const schedule = children.find(route => route.path === 'batches/:id/schedule');
    expect(sessions?.canActivate).toBeDefined();
    expect(schedule?.canActivate).toBeDefined();
    const sessionsGuard = sessions!.canActivate![0] as ReturnType<typeof roleGuard>;
    const scheduleGuard = schedule!.canActivate![0] as ReturnType<typeof roleGuard>;
    expect(TestBed.runInInjectionContext(() => sessionsGuard({} as never, state('/sessions')))).toBe(true);
    const denied = TestBed.runInInjectionContext(() => scheduleGuard({} as never, state('/batches/7/schedule')));
    expect(router.serializeUrl(denied as never)).toBe('/forbidden');
  });
});
