import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { Branch } from '../branches/branch.models';
import { BranchService } from '../branches/branch.service';
import { errorMessage, fieldError } from '../shared/api-error';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { HasUnsavedChanges } from '../shared/unsaved-changes.guard';
import {
  TemporaryPasswordDialogComponent,
} from './temporary-password-dialog.component';
import { Role, UserAccount, UserRequest } from './user.models';
import { UserService } from './user.service';

@Component({
  selector: 'app-user-form',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    PageHeaderComponent,
    PageStateComponent,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './user-form.component.html',
  styleUrl: './user-form.component.scss',
})
export class UserFormComponent implements OnInit, HasUnsavedChanges {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly branchService = inject(BranchService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private saved = false;

  protected readonly roles = signal<Role[]>([]);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverError = signal<unknown>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly loadedUser = signal<UserAccount | null>(null);
  protected readonly passwordVisible = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.email, Validators.maxLength(200)]],
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    contactNumber: ['', [Validators.maxLength(30)]],
    status: ['ACTIVE', [Validators.required]],
    temporaryPassword: ['', [Validators.minLength(8), Validators.maxLength(128)]],
    roleCodes: this.fb.nonNullable.control<string[]>([], [Validators.required]),
    branchIds: this.fb.nonNullable.control<number[]>([], [Validators.required]),
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.editingId.set(id);
    }
    this.load();
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.saved && !this.saving();
  }

  @HostListener('window:beforeunload', ['$event'])
  protected beforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = '';
    }
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    const id = this.editingId();
    forkJoin({
      roles: this.userService.roles(),
      branches: this.branchService.list({ status: 'ACTIVE', size: 100 }),
      user: id ? this.userService.get(id) : of(null),
    }).subscribe({
      next: ({ roles, branches, user }) => {
        this.roles.set(roles);
        this.branches.set(branches.content);
        if (user) {
          this.loadedUser.set(user);
          this.form.patchValue({
            username: user.username,
            email: user.email ?? '',
            fullName: user.fullName,
            contactNumber: user.contactNumber ?? '',
            status: user.status,
            roleCodes: user.roles.map((role) => role.code),
            branchIds: user.branches.map((branch) => branch.id),
          });
          this.form.controls.temporaryPassword.disable();
        } else {
          if (roles.length === 1) {
            this.form.controls.roleCodes.setValue([roles[0].code]);
          }
          if (branches.content.length === 1) {
            this.form.controls.branchIds.setValue([branches.content[0].id]);
          }
        }
        this.form.markAsPristine();
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.serverError.set(null);
    const id = this.editingId();
    if (id) {
      this.userService.updateProfile(id, {
        username: this.form.controls.username.value,
        email: this.form.controls.email.value || null,
        fullName: this.form.controls.fullName.value,
        contactNumber: this.form.controls.contactNumber.value || null,
      }).pipe(
        switchMap(() => this.userService.replaceRoles(id, this.form.controls.roleCodes.value)),
        switchMap(() => this.userService.replaceBranches(id, this.form.controls.branchIds.value)),
      ).subscribe({
        next: () => {
          this.saved = true;
          this.notifications.success('User updated successfully');
          this.router.navigateByUrl('/users');
        },
        error: (error) => this.handleSaveError(error),
      });
      return;
    }

    const request: UserRequest = {
      username: this.form.controls.username.value,
      email: this.form.controls.email.value || null,
      fullName: this.form.controls.fullName.value,
      contactNumber: this.form.controls.contactNumber.value || null,
      status: this.form.controls.status.value as 'ACTIVE' | 'DISABLED' | 'PASSWORD_CHANGE_REQUIRED',
      roleCodes: this.form.controls.roleCodes.value,
      branchIds: this.form.controls.branchIds.value,
      temporaryPassword: this.form.controls.temporaryPassword.value || null,
    };
    this.userService.create(request).subscribe({
      next: (user) => {
        this.saved = true;
        this.saving.set(false);
        this.notifications.success('User created successfully');
        if (user.temporaryPassword) {
          this.openTemporaryPassword(user.username, user.temporaryPassword);
        } else {
          this.router.navigateByUrl('/users');
        }
      },
      error: (error) => this.handleSaveError(error),
    });
  }

  protected toggleRole(code: string, checked: boolean): void {
    const values = new Set(this.form.controls.roleCodes.value);
    checked ? values.add(code) : values.delete(code);
    this.form.controls.roleCodes.setValue([...values]);
    this.form.controls.roleCodes.markAsTouched();
    this.form.controls.roleCodes.markAsDirty();
  }

  protected toggleBranch(id: number, checked: boolean): void {
    const values = new Set(this.form.controls.branchIds.value);
    checked ? values.add(id) : values.delete(id);
    this.form.controls.branchIds.setValue([...values]);
    this.form.controls.branchIds.markAsTouched();
    this.form.controls.branchIds.markAsDirty();
  }

  protected hasRole(code: string): boolean {
    return this.form.controls.roleCodes.value.includes(code);
  }

  protected hasBranch(id: number): boolean {
    return this.form.controls.branchIds.value.includes(id);
  }

  protected roleDescription(code: string): string {
    switch (code) {
      case 'SUPER_ADMIN':
        return 'Full administration access across every branch.';
      case 'ADMIN':
        return 'Manages academic and operational work within assigned branches.';
      case 'LECTURER':
        return 'Accesses assigned batches, sessions, and attendance only.';
      default:
        return 'Access is controlled by this role’s configured permissions.';
    }
  }

  protected fieldError(field: string): string | null {
    return fieldError(this.serverError(), field);
  }

  private openTemporaryPassword(username: string, temporaryPassword: string): void {
    this.dialog.open(TemporaryPasswordDialogComponent, {
      data: { username, temporaryPassword, title: 'User created' },
      disableClose: true,
      maxWidth: '36rem',
      width: 'calc(100vw - 2rem)',
    }).afterClosed().subscribe(() => this.router.navigateByUrl('/users'));
  }

  private handleSaveError(error: unknown): void {
    this.serverError.set(error);
    this.applyServerFieldErrors(error);
    const message = errorMessage(error);
    this.error.set(message);
    this.notifications.error(message);
    this.saving.set(false);
  }

  private applyServerFieldErrors(error: unknown): void {
    const response = error as { error?: { fieldErrors?: Array<{ field: string }> } };
    for (const item of response.error?.fieldErrors ?? []) {
      const control: AbstractControl | null = this.form.get(item.field);
      if (control) {
        control.setErrors({ ...control.errors, server: true });
        control.markAsTouched();
      }
    }
  }
}
