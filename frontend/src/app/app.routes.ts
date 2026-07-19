import { Routes } from '@angular/router';
import { ChangePasswordComponent } from './auth/change-password.component';
import { AccessDeniedComponent } from './auth/access-denied.component';
import { LoginComponent } from './auth/login.component';
import { BatchFormComponent } from './batches/batch-form.component';
import { BatchListComponent } from './batches/batch-list.component';
import { BranchFormComponent } from './branches/branch-form.component';
import { BranchListComponent } from './branches/branch-list.component';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guard';
import { CourseFormComponent } from './courses/course-form.component';
import { CourseListComponent } from './courses/course-list.component';
import { ShellComponent } from './dashboard/shell.component';
import { DashboardHomeComponent } from './dashboard/dashboard-home.component';
import { UserFormComponent } from './users/user-form.component';
import { UserListComponent } from './users/user-list.component';
import { unsavedChangesGuard } from './shared/unsaved-changes.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'change-password', component: ChangePasswordComponent, canActivate: [authGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', component: DashboardHomeComponent },
      { path: 'forbidden', component: AccessDeniedComponent },
      { path: 'branches', component: BranchListComponent, canActivate: [roleGuard(['SUPER_ADMIN'])] },
      {
        path: 'branches/new',
        component: BranchFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'branches/:id/edit',
        component: BranchFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      { path: 'users', component: UserListComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      { path: 'users/new', component: UserFormComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      { path: 'users/:id/edit', component: UserFormComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      { path: 'courses', component: CourseListComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      {
        path: 'courses/new',
        component: CourseFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'courses/:id/edit',
        component: CourseFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      { path: 'batches', component: BatchListComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      {
        path: 'batches/new',
        component: BatchFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'batches/:id/edit',
        component: BatchFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
