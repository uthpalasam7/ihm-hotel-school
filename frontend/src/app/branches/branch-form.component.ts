import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage, fieldError } from '../shared/api-error';
import { BranchService } from './branch.service';

@Component({
  selector: 'app-branch-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './branch-form.component.html',
  styleUrl: './branch-form.component.scss',
})
export class BranchFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly branchService = inject(BranchService);

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
    this.loading.set(true);
    this.branchService.get(id).subscribe({
      next: (branch) => {
        this.form.patchValue({
          code: branch.code,
          name: branch.name,
          address: branch.address ?? '',
          contactNumber: branch.contactNumber ?? '',
          status: branch.status,
        });
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
      next: () => this.router.navigateByUrl('/branches'),
      error: (error) => {
        this.serverError.set(error);
        this.error.set(errorMessage(error));
        this.saving.set(false);
      },
    });
  }

  protected fieldError(field: string): string | null {
    return fieldError(this.serverError(), field);
  }
}
