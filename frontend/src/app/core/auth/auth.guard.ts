import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.authenticated()) {
    return router.createUrlTree(['/login']);
  }
  if (auth.requiresPasswordChange() && state.url !== '/change-password') {
    return router.createUrlTree(['/change-password']);
  }
  return true;
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.authenticated()) {
    return true;
  }
  return auth.requiresPasswordChange()
    ? router.createUrlTree(['/change-password'])
    : router.createUrlTree(['/']);
};
