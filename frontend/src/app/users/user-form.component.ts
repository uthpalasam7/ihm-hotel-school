import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { Branch } from '../branches/branch.models';
import { BranchService } from '../branches/branch.service';
import { errorMessage, fieldError } from '../shared/api-error';
import { Role, UserAccount, UserRequest } from './user.models';
import { UserService } from './user.service';

@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-form.component.html',
  styleUrl: './user-form.component.scss',
})
export class UserFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly branchService = inject(BranchService);

  protected readonly roles = signal<Role[]>([]);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverError = signal<unknown>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly loadedUser = signal<UserAccount | null>(null);
  protected readonly temporaryPassword = signal<string | null>(null);

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
    this.loading.set(true);
    this.userService.roles().subscribe({
      next: (roles) => {
        this.roles.set(roles);
        if (!this.form.controls.roleCodes.value.length && roles.length === 1) {
          this.form.controls.roleCodes.setValue([roles[0].code]);
        }
      },
    });
    this.branchService.list({ status: 'ACTIVE', size: 100 }).subscribe({
      next: (page) => {
        this.branches.set(page.content);
        if (!this.form.controls.branchIds.value.length && page.content.length === 1) {
          this.form.controls.branchIds.setValue([page.content[0].id]);
        }
      },
    });

    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.loading.set(false);
      return;
    }
    this.editingId.set(id);
    this.userService.get(id).subscribe({
      next: (user) => {
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
    if (this.form.invalid) {
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
        next: () => this.router.navigateByUrl('/users'),
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
        this.temporaryPassword.set(user.temporaryPassword ?? null);
        this.saving.set(false);
      },
      error: (error) => this.handleSaveError(error),
    });
  }

  protected toggleRole(code: string, checked: boolean): void {
    const values = new Set(this.form.controls.roleCodes.value);
    checked ? values.add(code) : values.delete(code);
    this.form.controls.roleCodes.setValue([...values]);
    this.form.controls.roleCodes.markAsTouched();
  }

  protected toggleBranch(id: number, checked: boolean): void {
    const values = new Set(this.form.controls.branchIds.value);
    checked ? values.add(id) : values.delete(id);
    this.form.controls.branchIds.setValue([...values]);
    this.form.controls.branchIds.markAsTouched();
  }

  protected hasRole(code: string): boolean {
    return this.form.controls.roleCodes.value.includes(code);
  }

  protected hasBranch(id: number): boolean {
    return this.form.controls.branchIds.value.includes(id);
  }

  protected fieldError(field: string): string | null {
    return fieldError(this.serverError(), field);
  }

  private handleSaveError(error: unknown): void {
    this.serverError.set(error);
    this.error.set(errorMessage(error));
    this.saving.set(false);
  }
}
