import { Routes } from '@angular/router';
import { ChangePasswordComponent } from './auth/change-password.component';
import { AccessDeniedComponent } from './auth/access-denied.component';
import { LoginComponent } from './auth/login.component';
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
import { StudentFormComponent } from './students/student-form.component';
import { StudentListComponent } from './students/student-list.component';
import { StudentProfileComponent } from './students/student-profile.component';

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
      {
        path: 'users/new',
        component: UserFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'users/:id/edit',
        component: UserFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
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
      { path: 'batches', component: BatchListComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN', 'LECTURER'])] },
      { path: 'batches/:id/students', loadComponent: () => import('./enrollments/batch-students.component').then(m => m.BatchStudentsComponent), canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN', 'LECTURER'])] },
      {
        path: 'batches/new',
        loadComponent: () => import('./batches/batch-form.component').then(m => m.BatchFormComponent),
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'batches/:id/edit',
        loadComponent: () => import('./batches/batch-form.component').then(m => m.BatchFormComponent),
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      { path: 'students/:id/card', loadComponent: () => import('./cards/student-card.component').then(m => m.StudentCardComponent), canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      { path: 'enrollments', loadComponent: () => import('./enrollments/enrollment-list.component').then(m => m.EnrollmentListComponent), canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      { path: 'enrollments/new', loadComponent: () => import('./enrollments/enrollment-form.component').then(m => m.EnrollmentFormComponent), canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])], canDeactivate: [unsavedChangesGuard] },
      { path: 'enrollments/:id', loadComponent: () => import('./enrollments/enrollment-detail.component').then(m => m.EnrollmentDetailComponent), canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      { path: 'students', component: StudentListComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
      {
        path: 'students/new',
        component: StudentFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'students/:id/edit',
        component: StudentFormComponent,
        canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])],
        canDeactivate: [unsavedChangesGuard],
      },
      { path: 'students/:id', component: StudentProfileComponent, canActivate: [roleGuard(['SUPER_ADMIN', 'ADMIN'])] },
    ],
  },
  { path: '**', redirectTo: '' },
];
