import { Routes } from '@angular/router';
import { ChangePasswordComponent } from './auth/change-password.component';
import { LoginComponent } from './auth/login.component';
import { authGuard, guestGuard } from './core/auth/auth.guard';
import { ShellComponent } from './dashboard/shell.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'change-password', component: ChangePasswordComponent, canActivate: [authGuard] },
  { path: '', component: ShellComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' },
];
