import { DatePipe } from '@angular/common';
import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { filter, finalize, forkJoin, switchMap } from 'rxjs';
import { Branch } from '../branches/branch.models';
import { BranchService } from '../branches/branch.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { AuthService } from '../core/auth/auth.service';
import { errorMessage } from '../shared/api-error';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogResult,
} from '../shared/confirmation-dialog.component';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import {
  TemporaryPasswordDialogComponent,
} from './temporary-password-dialog.component';
import { Role, UserAccount, UserStatus } from './user.models';
import { UserService } from './user.service';

@Component({
  selector: 'app-user-list',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
    PageHeaderComponent,
    PageStateComponent,
    RouterLink,
    StatusChipComponent,
  ],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
})
export class UserListComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly branchService = inject(BranchService);
  private readonly activeBranchService = inject(ActiveBranchService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  private initialized = false;

  protected readonly users = signal<UserAccount[]>([]);
  protected readonly roles = signal<Role[]>([]);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly loading = signal(false);
  protected readonly updatingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly total = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly displayedColumns = ['user', 'roles', 'branches', 'status', 'lastLogin', 'actions'];

  protected search = '';
  protected role = '';
  protected branchId = '';
  protected status = '';

  constructor() {
    effect(() => {
      this.activeBranchService.activeBranchId();
      if (this.initialized) {
        this.pageIndex.set(0);
        this.load();
      }
    });
  }

  ngOnInit(): void {
    this.loading.set(true);
    forkJoin({
      roles: this.userService.roles(),
      branches: this.branchService.list({ status: 'ACTIVE', size: 100 }),
    }).subscribe({
      next: ({ roles, branches }) => {
        this.roles.set(roles);
        this.branches.set(branches.content);
        this.initialized = true;
        this.load();
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.userService.list({
      search: this.search.trim(),
      role: this.role,
      branchId: this.branchId,
      status: this.status,
      page: this.pageIndex(),
      size: this.pageSize(),
    }).subscribe({
      next: (page) => {
        this.users.set(page.content);
        this.total.set(page.totalElements);
        this.pageIndex.set(page.page);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected applyFilters(): void {
    this.pageIndex.set(0);
    this.load();
  }

  protected clearFilters(): void {
    this.search = '';
    this.role = '';
    this.branchId = '';
    this.status = '';
    this.applyFilters();
  }

  protected changePage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected currentBranchLabel(): string {
    const branch = this.activeBranchService.activeBranch();
    return branch ? `Current branch — ${branch.code}` : 'Current branch';
  }

  protected isCurrentUser(user: UserAccount): boolean {
    return this.authService.currentUser()?.id === user.id;
  }

  protected changeStatus(user: UserAccount): void {
    const status: UserStatus = user.status === 'DISABLED' ? 'ACTIVE' : 'DISABLED';
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(
      ConfirmationDialogComponent,
      {
        data: {
          title: `${status === 'ACTIVE' ? 'Activate' : 'Deactivate'} ${user.fullName}?`,
          message: status === 'DISABLED'
            ? 'This will prevent sign-in and revoke the user’s active sessions. Their history and assignments will be preserved.'
            : 'This will restore the account. The user may still need to change their password before continuing.',
          confirmLabel: status === 'ACTIVE' ? 'Activate user' : 'Deactivate user',
          reasonLabel: 'Reason (optional)',
        },
        maxWidth: '34rem',
        width: 'calc(100vw - 2rem)',
      },
    ).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed)),
      switchMap((result) => {
        this.updatingId.set(user.id);
        this.error.set(null);
        return this.userService.changeStatus(user.id, status, result.reason);
      }),
      finalize(() => this.updatingId.set(null)),
    ).subscribe({
      next: () => {
        this.notifications.success(`${user.username} status updated`);
        this.load();
      },
      error: (error) => this.handleActionError(error),
    });
  }

  protected resetPassword(user: UserAccount): void {
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(
      ConfirmationDialogComponent,
      {
        data: {
          title: `Reset ${user.fullName}’s password?`,
          message: 'A new temporary password will be generated and all active sessions will be revoked.',
          confirmLabel: 'Reset password',
          reasonLabel: 'Reason (optional)',
        },
        maxWidth: '34rem',
        width: 'calc(100vw - 2rem)',
      },
    ).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed)),
      switchMap((result) => {
        this.updatingId.set(user.id);
        this.error.set(null);
        return this.userService.resetPassword(user.id, null, result.reason);
      }),
      finalize(() => this.updatingId.set(null)),
    ).subscribe({
      next: (response) => {
        this.notifications.success(`${user.username} password reset`);
        this.load();
        if (response.temporaryPassword) {
          this.openTemporaryPassword(user.username, response.temporaryPassword);
        }
      },
      error: (error) => this.handleActionError(error),
    });
  }

  private openTemporaryPassword(username: string, temporaryPassword: string): void {
    this.dialog.open(TemporaryPasswordDialogComponent, {
      data: { username, temporaryPassword, title: 'Password reset complete' },
      disableClose: true,
      maxWidth: '36rem',
      width: 'calc(100vw - 2rem)',
    });
  }

  private handleActionError(error: unknown): void {
    const message = errorMessage(error);
    this.error.set(message);
    this.notifications.error(message);
  }
}
