import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Branch } from '../branches/branch.models';
import { BranchService } from '../branches/branch.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { errorMessage } from '../shared/api-error';
import { Role, UserAccount, UserStatus } from './user.models';
import { UserService } from './user.service';

@Component({
  selector: 'app-user-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
})
export class UserListComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly branchService = inject(BranchService);
  private readonly activeBranchService = inject(ActiveBranchService);
  private initialized = false;

  protected readonly users = signal<UserAccount[]>([]);
  protected readonly roles = signal<Role[]>([]);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly temporaryPassword = signal<string | null>(null);

  protected search = '';
  protected role = '';
  protected branchId = '';
  protected status = '';

  constructor() {
    effect(() => {
      this.activeBranchService.activeBranchId();
      if (this.initialized) {
        this.load();
      }
    });
  }

  ngOnInit(): void {
    this.userService.roles().subscribe({ next: (roles) => this.roles.set(roles) });
    this.branchService.list({ status: 'ACTIVE', size: 100 }).subscribe({ next: (page) => this.branches.set(page.content) });
    this.initialized = true;
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.userService.list({
      search: this.search.trim(),
      role: this.role,
      branchId: this.branchId,
      status: this.status,
    }).subscribe({
      next: (page) => {
        this.users.set(page.content);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected changeStatus(user: UserAccount): void {
    const status: UserStatus = user.status === 'DISABLED' ? 'ACTIVE' : 'DISABLED';
    const reason = window.prompt(`Reason for marking ${user.username} ${status.toLowerCase()}:`);
    if (reason === null) {
      return;
    }
    this.loading.set(true);
    this.userService.changeStatus(user.id, status, reason).subscribe({
      next: () => {
        this.success.set(`${user.username} status updated`);
        this.load();
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected resetPassword(user: UserAccount): void {
    const reason = window.prompt(`Reason for resetting ${user.username}'s password:`);
    if (reason === null) {
      return;
    }
    this.loading.set(true);
    this.userService.resetPassword(user.id, null, reason).subscribe({
      next: (response) => {
        this.temporaryPassword.set(response.temporaryPassword ?? null);
        this.success.set(`${user.username} password reset. Temporary password is shown below once.`);
        this.load();
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }
}
