import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage, fieldError } from '../shared/api-error';
import { CourseService } from './course.service';

@Component({
  selector: 'app-course-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './course-form.component.html',
  styleUrl: './course-form.component.scss',
})
export class CourseFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverError = signal<unknown>(null);
  protected readonly editingId = signal<number | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    shortCode: ['', [Validators.required, Validators.maxLength(20)]],
    description: [''],
    status: ['ACTIVE', [Validators.required]],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      return;
    }
    this.editingId.set(id);
    this.loading.set(true);
    this.courseService.get(id).subscribe({
      next: (course) => {
        this.form.patchValue({
          name: course.name,
          shortCode: course.shortCode,
          description: course.description ?? '',
          status: course.status,
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
      name: this.form.controls.name.value,
      shortCode: this.form.controls.shortCode.value,
      description: this.form.controls.description.value || null,
      status: this.form.controls.status.value as 'ACTIVE' | 'INACTIVE',
    };
    const id = this.editingId();
    const action = id ? this.courseService.update(id, request) : this.courseService.create(request);
    action.subscribe({
      next: () => this.router.navigateByUrl('/courses'),
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
