import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { DateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Batch } from '../batches/batch.models';
import { BatchService } from '../batches/batch.service';
import { Student } from '../students/student.models';
import { StudentService } from '../students/student.service';
import { errorMessage } from '../shared/api-error';
import { DayMonthYearDateAdapter } from '../shared/day-month-year-date-adapter';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { EnrollmentPreview, EnrollmentRequest } from './enrollment.models';
import { EnrollmentService } from './enrollment.service';

@Component({
  selector: 'app-enrollment-form',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, MatButtonModule, MatCardModule,
    MatDatepickerModule, MatFormFieldModule, MatInputModule, MatPaginatorModule, MatTableModule, RouterLink,
    PageHeaderComponent, PageStateComponent],
  providers: [{ provide: DateAdapter, useClass: DayMonthYearDateAdapter }, { provide: MAT_DATE_LOCALE, useValue: 'en-GB' }],
  templateUrl: './enrollment-form.component.html',
  styleUrl: './enrollments.scss',
})
export class EnrollmentFormComponent implements OnInit {
  protected readonly displayedChargeColumns = ['description', 'dueDate', 'amount'];
  private readonly api = inject(EnrollmentService);
  private readonly studentApi = inject(StudentService);
  private readonly batchApi = inject(BatchService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private previewSubscription?: Subscription;
  private studentSubscription?: Subscription;
  private batchSubscription?: Subscription;
  protected readonly form = this.fb.group({
    studentId: this.fb.control<number | null>(null, Validators.required),
    batchId: this.fb.control<number | null>(null, Validators.required),
    enrollmentDate: this.fb.control<Date | null>(this.today(), Validators.required),
    remarks: this.fb.nonNullable.control('', Validators.maxLength(2000)),
  });
  protected readonly studentSearch = this.fb.nonNullable.control('');
  protected readonly batchSearch = this.fb.nonNullable.control('');
  protected readonly selectedStudent = signal<Student | null>(null);
  protected readonly selectedBatch = signal<Batch | null>(null);
  protected readonly students = signal<Student[]>([]);
  protected readonly batches = signal<Batch[]>([]);
  protected readonly studentTotal = signal(0);
  protected readonly batchTotal = signal(0);
  protected readonly studentPage = signal(0);
  protected readonly batchPage = signal(0);
  protected readonly studentLoading = signal(false);
  protected readonly batchLoading = signal(false);
  protected readonly studentError = signal<string | null>(null);
  protected readonly batchError = signal<string | null>(null);
  protected readonly preview = signal<EnrollmentPreview | null>(null);
  protected readonly previewing = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.previewSubscription?.unsubscribe();
      this.preview.set(null);
      this.previewing.set(false);
      this.error.set(null);
    });
  }
  ngOnInit(): void {
    const studentId = Number(this.route.snapshot.queryParamMap.get('studentId'));
    const batchId = Number(this.route.snapshot.queryParamMap.get('batchId'));
    if (studentId > 0) {
      this.studentLoading.set(true);
      this.studentApi.get(studentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: student => { this.selectStudent(student); this.studentLoading.set(false); },
        error: error => { this.studentError.set(errorMessage(error)); this.studentLoading.set(false); },
      });
    } else this.searchStudents();
    if (batchId > 0) {
      this.batchLoading.set(true);
      this.batchApi.get(batchId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: batch => { this.selectBatch(batch); this.batchLoading.set(false); },
        error: error => { this.batchError.set(errorMessage(error)); this.batchLoading.set(false); },
      });
    } else this.searchBatches();
  }
  protected searchStudents(page = 0): void {
    this.studentSubscription?.unsubscribe();
    this.studentLoading.set(true); this.studentError.set(null); this.studentPage.set(page);
    this.studentSubscription = this.studentApi.list({ search: this.studentSearch.value.trim(), status: 'ACTIVE', page, size: 5 })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: result => { this.students.set(result.content); this.studentTotal.set(result.totalElements); this.studentLoading.set(false); },
        error: error => { this.studentError.set(errorMessage(error)); this.studentLoading.set(false); },
      });
  }
  protected searchBatches(page = 0): void {
    this.batchSubscription?.unsubscribe();
    this.batchLoading.set(true); this.batchError.set(null); this.batchPage.set(page);
    this.batchSubscription = this.batchApi.list({ search: this.batchSearch.value.trim(), page, size: 5 })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: result => { this.batches.set(result.content); this.batchTotal.set(result.totalElements); this.batchLoading.set(false); },
        error: error => { this.batchError.set(errorMessage(error)); this.batchLoading.set(false); },
      });
  }
  protected selectStudent(student: Student | null): void {
    this.selectedStudent.set(student); this.form.controls.studentId.setValue(student?.id ?? null);
    this.form.markAsDirty(); if (!student) this.searchStudents();
  }
  protected selectBatch(batch: Batch | null): void {
    this.selectedBatch.set(batch); this.form.controls.batchId.setValue(batch?.id ?? null);
    this.form.markAsDirty(); if (!batch) this.searchBatches();
  }
  protected eligible(batch: Batch): boolean { return ['ACTIVE', 'UPCOMING'].includes(batch.status) && batch.feePlanConfigured; }
  protected previewCharges(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.selectedStudent()?.status !== 'ACTIVE' || !this.selectedBatch() || !this.eligible(this.selectedBatch()!) || this.previewing() || this.saving()) return;
    this.preview.set(null); this.error.set(null); this.previewing.set(true);
    this.previewSubscription = this.api.preview(this.request()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: preview => { this.preview.set(preview); this.previewing.set(false); },
      error: error => { this.error.set(errorMessage(error)); this.previewing.set(false); },
    });
  }
  protected enroll(): void {
    const preview = this.preview();
    if (!preview || this.form.invalid || this.saving()) return;
    const request = { ...this.request(), expectedBatchVersion: preview.batchVersion, expectedFeePlanVersion: preview.feePlanVersion };
    this.saving.set(true); this.error.set(null); this.form.disable({ emitEvent: false });
    this.api.create(request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: enrollment => {
        this.form.markAsPristine(); this.saving.set(false);
        this.notifications.success(`Enrollment created: ${enrollment.registrationNumber}`);
        this.router.navigate(['/enrollments', enrollment.id]);
      },
      error: error => {
        this.form.enable({ emitEvent: false }); this.saving.set(false); this.preview.set(null);
        this.error.set(errorMessage(error));
      },
    });
  }
  hasUnsavedChanges(): boolean { return this.form.dirty; }
  private request(): EnrollmentRequest {
    const value = this.form.getRawValue(); const date = value.enrollmentDate!;
    return { studentId: value.studentId!, batchId: value.batchId!, remarks: value.remarks.trim(),
      enrollmentDate: `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}` };
  }
  private today(): Date {
    const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Colombo', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
    const part = (type: string) => Number(parts.find(p => p.type === type)?.value);
    return new Date(part('year'), part('month') - 1, part('day'));
  }
}
