import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage, fieldError } from '../shared/api-error';
import { HasUnsavedChanges } from '../shared/unsaved-changes.guard';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { BranchService } from './branch.service';

@Component({
  selector: 'app-branch-form',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    PageHeaderComponent,
    PageStateComponent,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './branch-form.component.html',
  styleUrl: './branch-form.component.scss',
})
export class BranchFormComponent implements OnInit, HasUnsavedChanges {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly branchService = inject(BranchService);
  private readonly notifications = inject(NotificationService);
  private saved = false;

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverError = signal<unknown>(null);
  protected readonly editingId = signal<number | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(30)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    address: [''],
    contactNumber: ['', [Validators.maxLength(30)]],
    status: ['ACTIVE', [Validators.required]],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      return;
    }
    this.editingId.set(id);
    this.loadBranch();
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

  protected loadBranch(): void {
    const id = this.editingId();
    if (!id) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.branchService.get(id).subscribe({
      next: (branch) => {
        this.form.patchValue({
          code: branch.code,
          name: branch.name,
          address: branch.address ?? '',
          contactNumber: branch.contactNumber ?? '',
          status: branch.status,
        });
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
    const request = {
      code: this.form.controls.code.value,
      name: this.form.controls.name.value,
      address: this.form.controls.address.value || null,
      contactNumber: this.form.controls.contactNumber.value || null,
      status: this.form.controls.status.value as 'ACTIVE' | 'INACTIVE',
    };
    const id = this.editingId();
    const action = id ? this.branchService.update(id, request) : this.branchService.create(request);
    action.subscribe({
      next: () => {
        this.saved = true;
        this.notifications.success(id ? 'Branch updated successfully' : 'Branch created successfully');
        this.router.navigateByUrl('/branches');
      },
      error: (error) => {
        this.serverError.set(error);
        this.applyServerFieldErrors(error);
        const message = errorMessage(error);
        this.error.set(message);
        this.notifications.error(message);
        this.saving.set(false);
      },
    });
  }

  protected fieldError(field: string): string | null {
    return fieldError(this.serverError(), field);
  }

  private applyServerFieldErrors(error: unknown): void {
    const response = error as { error?: { fieldErrors?: Array<{ field: string }> } };
    for (const item of response.error?.fieldErrors ?? []) {
      const control = this.form.get(item.field);
      if (control) {
        control.setErrors({ ...control.errors, server: true });
        control.markAsTouched();
      }
    }
  }
}
