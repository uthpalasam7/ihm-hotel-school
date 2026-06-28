import { Routes } from '@angular/router';
import { ChangePasswordComponent } from './auth/change-password.component';
import { LoginComponent } from './auth/login.component';
import { BranchFormComponent } from './branches/branch-form.component';
import { BranchListComponent } from './branches/branch-list.component';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guard';
import { ShellComponent } from './dashboard/shell.component';
import { DashboardHomeComponent } from './dashboard/dashboard-home.component';
import { UserFormComponent } from './users/user-form.component';
import { UserListComponent } from './users/user-list.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'change-password', component: ChangePasswordComponent, canActivate: [authGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', component: DashboardHomeComponent },
      { path: 'branches', component: BranchListComponent, canActivate: [roleGuard(['SUPER_ADMIN'])] },
      { path: 'branches/new', component: BranchFormComponent, canActivate: [roleGuard(['SUPER_ADMIN'])] },
      { path: 'branches/:id/edit', component: BranchFormComponent, canActivate: [roleGuard(['SUPER_ADMIN'])] },
      { path: 'users', component: UserListComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      { path: 'users/new', component: UserFormComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      { path: 'users/:id/edit', component: UserFormComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
    ],
  },
  { path: '**', redirectTo: '' },
];
