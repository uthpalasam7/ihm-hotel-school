import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage, fieldError } from '../shared/api-error';
import { DateValue, toIsoDate, toLocalDate } from '../shared/date-value';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { HasUnsavedChanges } from '../shared/unsaved-changes.guard';
import { Student, StudentRequest } from './student.models';
import { StudentPhotoComponent } from './student-photo.component';
import { StudentService } from './student.service';

const MAX_PHOTO_SIZE = 5 * 1024 * 1024;
const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/png'];
const CONTACT_NUMBER_PATTERN = /^\d{10}$/;
const NIC_PATTERN = /^[A-Z0-9]+$/;

@Component({
  selector: 'app-student-form',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    PageHeaderComponent,
    PageStateComponent,
    ReactiveFormsModule,
    RouterLink,
    StudentPhotoComponent,
  ],
  templateUrl: './student-form.component.html',
  styleUrl: './student-form.component.scss',
})
export class StudentFormComponent implements OnInit, OnDestroy, HasUnsavedChanges {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly studentService = inject(StudentService);
  private readonly notifications = inject(NotificationService);
  private saved = false;
  private nicLookupSequence = 0;

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly checkingNic = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverError = signal<unknown>(null);
  protected readonly nicLookupError = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly loadedStudent = signal<Student | null>(null);
  protected readonly existingStudent = signal<Student | null>(null);
  protected readonly selectedPhoto = signal<File | null>(null);
  protected readonly photoPreview = signal<string | null>(null);
  protected readonly photoError = signal<string | null>(null);
  protected readonly genderOptions = ['Male', 'Female', 'Other'];

  protected readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(250)]],
    nic: ['', [Validators.required, Validators.maxLength(30), Validators.pattern(NIC_PATTERN)]],
    contactNumber: ['', [Validators.required, Validators.pattern(CONTACT_NUMBER_PATTERN)]],
    alternativeContactNumber: ['', [Validators.pattern(CONTACT_NUMBER_PATTERN)]],
    email: ['', [Validators.email, Validators.maxLength(200)]],
    address: ['', [Validators.required]],
    dateOfBirth: this.fb.control<DateValue>(null),
    gender: [''],
    remarks: [''],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.editingId.set(id);
      this.load();
    }
  }

  ngOnDestroy(): void {
    this.revokePreview();
  }

  hasUnsavedChanges(): boolean {
    return (this.form.dirty || Boolean(this.selectedPhoto())) && !this.saved && !this.saving();
  }

  @HostListener('window:beforeunload', ['$event'])
  protected beforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = '';
    }
  }

  protected load(): void {
    const id = this.editingId();
    if (!id) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.studentService.get(id).subscribe({
      next: (student) => {
        this.loadedStudent.set(student);
        this.form.patchValue({
          fullName: student.fullName,
          nic: student.nic,
          contactNumber: student.contactNumber,
          alternativeContactNumber: student.alternativeContactNumber ?? '',
          email: student.email ?? '',
          address: student.address,
          dateOfBirth: toLocalDate(student.dateOfBirth ?? null),
          gender: student.gender ?? '',
          remarks: student.remarks ?? '',
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

  protected checkNic(): void {
    this.normalizeNicInput();
    const control = this.form.controls.nic;
    if (control.invalid || !control.value.trim()) {
      this.existingStudent.set(null);
      return;
    }
    const sequence = ++this.nicLookupSequence;
    this.checkingNic.set(true);
    this.nicLookupError.set(null);
    this.studentService.findByNic(control.value).subscribe({
      next: (student) => {
        if (sequence !== this.nicLookupSequence) {
          return;
        }
        this.existingStudent.set(student.id === this.editingId() ? null : student);
        this.checkingNic.set(false);
      },
      error: (error: unknown) => {
        if (sequence !== this.nicLookupSequence) {
          return;
        }
        if (error instanceof HttpErrorResponse && error.status === 404) {
          this.existingStudent.set(null);
        } else {
          this.nicLookupError.set(errorMessage(error, 'NIC could not be checked'));
        }
        this.checkingNic.set(false);
      },
    });
  }

  protected normalizeNicInput(): void {
    const control = this.form.controls.nic;
    const normalized = control.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
    if (control.value !== normalized) {
      control.setValue(normalized, { emitEvent: false });
    }
  }

  protected normalizePhoneInput(controlName: 'contactNumber' | 'alternativeContactNumber'): void {
    const control = this.form.controls[controlName];
    const normalized = control.value.replace(/\D/g, '').slice(0, 10);
    if (control.value !== normalized) {
      control.setValue(normalized, { emitEvent: false });
    }
  }

  protected selectPhoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    this.photoError.set(null);
    if (!file) {
      return;
    }
    if (!ALLOWED_PHOTO_TYPES.includes(file.type)) {
      this.photoError.set('Choose a JPEG or PNG image.');
      return;
    }
    if (file.size > MAX_PHOTO_SIZE) {
      this.photoError.set('Student photo must be 5 MiB or smaller.');
      return;
    }
    this.revokePreview();
    this.selectedPhoto.set(file);
    this.photoPreview.set(URL.createObjectURL(file));
  }

  protected clearSelectedPhoto(): void {
    this.revokePreview();
    this.selectedPhoto.set(null);
    this.photoError.set(null);
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving() || this.existingStudent()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.serverError.set(null);
    const request = this.request();
    const id = this.editingId();
    const action = id ? this.studentService.update(id, request) : this.studentService.create(request);
    action.subscribe({
      next: (student) => this.finishWithPhoto(student),
      error: (error) => this.handleSaveError(error),
    });
  }

  protected fieldError(field: string): string | null {
    return fieldError(this.serverError(), field);
  }

  private finishWithPhoto(student: Student): void {
    const photo = this.selectedPhoto();
    if (!photo) {
      this.complete(student, false);
      return;
    }
    this.studentService.uploadPhoto(student.id, photo).subscribe({
      next: (updated) => this.complete(updated, false),
      error: (error) => {
        this.notifications.error(`Student saved, but the photo was not uploaded. ${errorMessage(error)}`);
        this.complete(student, true);
      },
    });
  }

  private complete(student: Student, partial: boolean): void {
    this.saved = true;
    this.saving.set(false);
    if (!partial) {
      this.notifications.success(this.editingId() ? 'Student updated successfully' : 'Student created successfully');
    }
    this.router.navigate(['/students', student.id]);
  }

  private request(): StudentRequest {
    return {
      fullName: this.form.controls.fullName.value,
      nic: this.form.controls.nic.value,
      contactNumber: this.form.controls.contactNumber.value,
      alternativeContactNumber: this.form.controls.alternativeContactNumber.value || null,
      email: this.form.controls.email.value || null,
      address: this.form.controls.address.value,
      dateOfBirth: toIsoDate(this.form.controls.dateOfBirth.value) || null,
      gender: this.form.controls.gender.value || null,
      remarks: this.form.controls.remarks.value || null,
    };
  }

  private handleSaveError(error: unknown): void {
    this.serverError.set(error);
    const response = error as { error?: { fieldErrors?: Array<{ field: string }> } };
    for (const item of response.error?.fieldErrors ?? []) {
      const control = this.form.get(item.field);
      if (control) {
        control.setErrors({ ...control.errors, server: true });
        control.markAsTouched();
      }
    }
    if (error instanceof HttpErrorResponse && error.status === 409) {
      this.checkNic();
    }
    const message = errorMessage(error);
    this.error.set(message);
    this.notifications.error(message);
    this.saving.set(false);
  }

  private revokePreview(): void {
    const preview = this.photoPreview();
    if (preview) {
      URL.revokeObjectURL(preview);
      this.photoPreview.set(null);
    }
  }
}
