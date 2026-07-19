import { DatePipe, DecimalPipe } from '@angular/common';
import { BreakpointObserver } from '@angular/cdk/layout';
import { StepperOrientation } from '@angular/cdk/stepper';
import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { Branch } from '../branches/branch.models';
import { BranchService } from '../branches/branch.service';
import { Course } from '../courses/course.models';
import { CourseService } from '../courses/course.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { errorMessage, fieldError } from '../shared/api-error';
import { DateValue, toIsoDate, toLocalDate } from '../shared/date-value';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import { HasUnsavedChanges } from '../shared/unsaved-changes.guard';
import { UserAccount } from '../users/user.models';
import { UserService } from '../users/user.service';
import { Batch, BatchLecturer, FeePlan, FeePlanRequest, PreviewCharge } from './batch.models';
import { BatchService } from './batch.service';

interface LecturerSummary {
  id: number;
  fullName: string;
  username: string;
}

function orderedDates(startControl: string, endControl: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const start = toIsoDate(control.get(startControl)?.value as DateValue);
    const end = toIsoDate(control.get(endControl)?.value as DateValue);
    return start && end && start > end ? { dateOrder: true } : null;
  };
}

@Component({
  selector: 'app-batch-form',
  imports: [
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    MatSelectModule,
    MatStepperModule,
    PageHeaderComponent,
    PageStateComponent,
    ReactiveFormsModule,
    RouterLink,
    StatusChipComponent,
  ],
  templateUrl: './batch-form.component.html',
  styleUrl: './batch-form.component.scss',
})
export class BatchFormComponent implements OnInit, HasUnsavedChanges {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly batchService = inject(BatchService);
  private readonly courseService = inject(CourseService);
  private readonly branchService = inject(BranchService);
  private readonly userService = inject(UserService);
  private readonly activeBranchService = inject(ActiveBranchService);
  private readonly notifications = inject(NotificationService);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private saved = false;
  private initialLecturerIds: number[] = [];

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverError = signal<unknown>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly courses = signal<Course[]>([]);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly lecturers = signal<UserAccount[]>([]);
  protected readonly lecturersLoading = signal(false);
  protected readonly lecturerTotal = signal(0);
  protected readonly lecturerPageIndex = signal(0);
  protected readonly lecturerPageSize = signal(10);
  protected readonly assignments = signal<BatchLecturer[]>([]);
  protected readonly selectedLecturerDetails = signal<LecturerSummary[]>([]);
  protected readonly lecturerSearch = signal('');
  protected selectedLecturerIds: number[] = [];

  protected readonly stepperOrientation = toSignal(
    this.breakpointObserver.observe('(max-width: 760px)').pipe(
      map((result): StepperOrientation => result.matches ? 'vertical' : 'horizontal'),
    ),
    { initialValue: 'horizontal' as StepperOrientation },
  );

  protected readonly generalForm = this.fb.nonNullable.group({
    courseId: [0, [Validators.required, Validators.min(1)]],
    branchId: [0, [Validators.required, Validators.min(1)]],
    batchNumber: ['', [Validators.required, Validators.maxLength(60)]],
    startDate: this.fb.control<DateValue>(null, Validators.required),
    endDate: this.fb.control<DateValue>(null, Validators.required),
    durationMonths: [6, [Validators.required, Validators.min(1)]],
    status: ['UPCOMING', [Validators.required]],
    remarks: [''],
  }, { validators: orderedDates('startDate', 'endDate') });

  protected readonly feeForm = this.fb.nonNullable.group({
    registrationFee: [0, [Validators.required, Validators.min(0)]],
    courseFee: [0, [Validators.required, Validators.min(0)]],
    examinationFee: [0, [Validators.required, Validators.min(0)]],
    monthlyDueDay: [10, [Validators.required, Validators.min(1), Validators.max(31)]],
    examinationDueDate: this.fb.control<DateValue>(null, Validators.required),
    currencyCode: ['LKR', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
  });

  protected readonly scheduleForm = this.fb.nonNullable.group({
    scheduleMode: ['REGULAR', [Validators.required]],
  });

  ngOnInit(): void {
    const activeBranch = this.activeBranchService.activeBranch();
    this.generalForm.patchValue({ branchId: activeBranch?.id ?? 0 });
    this.loading.set(true);
    forkJoin({
      courses: this.courseService.listAllActive(),
      branches: this.branchService.listAllActive(),
    }).subscribe({
      next: ({ courses, branches }) => {
        this.courses.set(courses);
        this.branches.set(branches);
        if (!this.generalForm.controls.courseId.value && courses[0]) {
          this.generalForm.patchValue({ courseId: courses[0].id });
        }
        if (!this.generalForm.controls.branchId.value && branches[0]) {
          this.generalForm.patchValue({ branchId: branches[0].id });
        }
        this.loadForEdit();
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  hasUnsavedChanges(): boolean {
    return (
      this.generalForm.dirty
      || this.feeForm.dirty
      || this.scheduleForm.dirty
      || this.lecturerSelectionChanged()
    ) && !this.saved && !this.saving();
  }

  @HostListener('window:beforeunload', ['$event'])
  protected beforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = '';
    }
  }

  protected next(stepper: MatStepper, control: AbstractControl): void {
    control.markAllAsTouched();
    if (control.invalid) {
      return;
    }
    stepper.next();
  }

  protected goToStep(stepper: MatStepper, index: number): void {
    stepper.selectedIndex = index;
  }

  protected lecturerInitials(fullName: string): string {
    return fullName
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('') || 'L';
  }

  protected selectedLecturerSummaries(): LecturerSummary[] {
    const lecturersById = new Map<number, LecturerSummary>();
    for (const lecturer of this.assignments()) {
      lecturersById.set(lecturer.lecturerUserId, {
        id: lecturer.lecturerUserId,
        fullName: lecturer.lecturerFullName,
        username: lecturer.lecturerUsername,
      });
    }
    for (const lecturer of this.selectedLecturerDetails()) {
      lecturersById.set(lecturer.id, lecturer);
    }
    for (const lecturer of this.lecturers()) {
      lecturersById.set(lecturer.id, {
        id: lecturer.id,
        fullName: lecturer.fullName,
        username: lecturer.username,
      });
    }
    return this.selectedLecturerIds.map((id) => lecturersById.get(id) ?? {
      id,
      fullName: `Lecturer ${id}`,
      username: 'Selected lecturer',
    });
  }

  protected isLecturerSelected(lecturerId: number): boolean {
    return this.selectedLecturerIds.includes(lecturerId);
  }

  protected toggleLecturer(lecturerId: number, selected: boolean): void {
    if (selected) {
      this.selectedLecturerIds = this.deduplicate([...this.selectedLecturerIds, lecturerId]);
      const lecturer = this.lecturers().find((item) => item.id === lecturerId);
      if (lecturer && !this.selectedLecturerDetails().some((item) => item.id === lecturerId)) {
        this.selectedLecturerDetails.update((items) => [...items, {
          id: lecturer.id,
          fullName: lecturer.fullName,
          username: lecturer.username,
        }]);
      }
    } else {
      this.removeSelectedLecturer(lecturerId);
    }
  }

  protected removeSelectedLecturer(lecturerId: number): void {
    this.selectedLecturerIds = this.selectedLecturerIds.filter((id) => id !== lecturerId);
    this.selectedLecturerDetails.update((items) => items.filter((item) => item.id !== lecturerId));
  }

  protected applyLecturerSearch(): void {
    this.lecturerPageIndex.set(0);
    this.loadLecturers();
  }

  protected changeLecturerPage(event: PageEvent): void {
    this.lecturerPageIndex.set(event.pageIndex);
    this.lecturerPageSize.set(event.pageSize);
    this.loadLecturers();
  }

  protected loadLecturers(): void {
    const branchId = this.generalForm.controls.branchId.value;
    if (!branchId) {
      this.lecturers.set([]);
      this.lecturerTotal.set(0);
      return;
    }
    this.lecturersLoading.set(true);
    this.userService.list({
      search: this.lecturerSearch().trim(),
      role: 'LECTURER',
      branchId,
      status: 'ACTIVE',
      page: this.lecturerPageIndex(),
      size: this.lecturerPageSize(),
    }).pipe(
      finalize(() => this.lecturersLoading.set(false)),
    ).subscribe({
      next: (page) => {
        this.lecturers.set(page.content);
        this.lecturerTotal.set(page.totalElements);
        this.lecturerPageIndex.set(page.page);
      },
      error: (error) => {
        const message = errorMessage(error);
        this.error.set(message);
        this.notifications.error(message);
      },
    });
  }

  protected branchChanged(): void {
    this.selectedLecturerIds = [];
    this.selectedLecturerDetails.set([]);
    this.assignments.set([]);
    this.lecturerSearch.set('');
    this.lecturerPageIndex.set(0);
    this.loadLecturers();
  }

  protected previewCharges(): PreviewCharge[] {
    const startDate = toIsoDate(this.generalForm.controls.startDate.value);
    const durationMonths = this.generalForm.controls.durationMonths.value;
    const monthlyDueDay = this.feeForm.controls.monthlyDueDay.value;
    const courseFee = this.money(this.feeForm.controls.courseFee.value);
    if (!startDate || durationMonths < 1) {
      return [];
    }
    const base = Math.floor((courseFee / durationMonths) * 100) / 100;
    const charges: PreviewCharge[] = [{
      type: 'REGISTRATION_FEE',
      description: 'Registration fee',
      dueDate: startDate,
      amount: this.money(this.feeForm.controls.registrationFee.value),
    }];
    let allocated = 0;
    for (let i = 1; i <= durationMonths; i++) {
      const amount = i === durationMonths ? this.money(courseFee - allocated) : base;
      allocated = this.money(allocated + amount);
      charges.push({
        type: 'COURSE_INSTALLMENT',
        installmentNumber: i,
        description: `Course installment ${i}`,
        dueDate: this.installmentDueDate(startDate, monthlyDueDay, i),
        amount,
      });
    }
    charges.push({
      type: 'EXAMINATION_FEE',
      description: 'Examination fee',
      dueDate: toIsoDate(this.feeForm.controls.examinationDueDate.value),
      amount: this.money(this.feeForm.controls.examinationFee.value),
    });
    return charges;
  }

  protected previewTotal(): number {
    return this.money(this.previewCharges().reduce((total, charge) => total + charge.amount, 0));
  }

  protected selectedCourse(): Course | undefined {
    return this.courses().find((course) => course.id === this.generalForm.controls.courseId.value);
  }

  protected selectedBranch(): Branch | undefined {
    return this.branches().find((branch) => branch.id === this.generalForm.controls.branchId.value);
  }

  protected save(): void {
    if (this.saving()) {
      return;
    }
    this.generalForm.markAllAsTouched();
    this.feeForm.markAllAsTouched();
    this.scheduleForm.markAllAsTouched();
    if (
      this.generalForm.invalid
      || this.feeForm.invalid
      || this.scheduleForm.invalid
    ) {
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.serverError.set(null);
    const batchRequest = {
      courseId: this.generalForm.controls.courseId.value,
      branchId: this.generalForm.controls.branchId.value,
      batchNumber: this.generalForm.controls.batchNumber.value,
      startDate: toIsoDate(this.generalForm.controls.startDate.value),
      endDate: toIsoDate(this.generalForm.controls.endDate.value),
      durationMonths: this.generalForm.controls.durationMonths.value,
      scheduleMode: this.scheduleForm.controls.scheduleMode.value as 'REGULAR' | 'MANUAL',
      status: this.generalForm.controls.status.value as 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED',
      remarks: this.generalForm.controls.remarks.value || null,
    };
    const id = this.editingId();
    const batchAction = id ? this.batchService.update(id, batchRequest) : this.batchService.create(batchRequest);
    batchAction.pipe(
      switchMap((batch) => this.batchService.saveFeePlan(batch.id, this.feePlanRequest()).pipe(
        switchMap(() => this.saveLecturers(batch)),
      )),
    ).subscribe({
      next: () => {
        this.saved = true;
        this.notifications.success(id ? 'Batch updated successfully' : 'Batch created successfully');
        this.router.navigateByUrl('/batches');
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

  private loadForEdit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.initialLecturerIds = [];
      this.markFormsPristine();
      this.loadLecturers();
      this.loading.set(false);
      return;
    }
    this.editingId.set(id);
    this.batchService.get(id).pipe(
      switchMap((batch) => forkJoin({
        batch: of(batch),
        feePlan: this.batchService.getFeePlan(id).pipe(catchError(() => of(null))),
        assignments: this.batchService.lecturers(id),
      })),
    ).subscribe({
      next: ({ batch, feePlan, assignments }) => {
        this.ensureCurrentOptions(batch);
        this.generalForm.patchValue({
          courseId: batch.course.id,
          branchId: batch.branch.id,
          batchNumber: batch.batchNumber,
          startDate: toLocalDate(batch.startDate),
          endDate: toLocalDate(batch.endDate),
          durationMonths: batch.durationMonths,
          status: batch.status,
          remarks: batch.remarks ?? '',
        });
        this.scheduleForm.patchValue({ scheduleMode: batch.scheduleMode });
        if (feePlan) {
          this.feeForm.patchValue(this.feePlanFormValue(feePlan));
        }
        this.assignments.set(assignments);
        const activeAssignments = assignments.filter((assignment) => assignment.status === 'ACTIVE');
        this.selectedLecturerIds = this.deduplicate(activeAssignments
          .map((assignment) => assignment.lecturerUserId));
        this.initialLecturerIds = [...this.selectedLecturerIds];
        this.selectedLecturerDetails.set(activeAssignments
          .map((assignment) => ({
            id: assignment.lecturerUserId,
            fullName: assignment.lecturerFullName,
            username: assignment.lecturerUsername,
          })));
        this.markFormsPristine();
        this.loadLecturers();
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  private saveLecturers(batch: Batch) {
    const selectedLecturerIds = this.deduplicate(this.selectedLecturerIds);
    if (!this.editingId() && selectedLecturerIds.length === 0) {
      return of([]);
    }
    return this.batchService.syncLecturers(batch.id, {
      lecturerUserIds: selectedLecturerIds,
      assignmentStartDate: toIsoDate(this.generalForm.controls.startDate.value),
      assignmentEndDate: null,
    });
  }

  private lecturerSelectionChanged(): boolean {
    const current = [...this.selectedLecturerIds].sort((left, right) => left - right);
    const initial = [...this.initialLecturerIds].sort((left, right) => left - right);
    return current.length !== initial.length || current.some((id, index) => id !== initial[index]);
  }

  private markFormsPristine(): void {
    this.generalForm.markAsPristine();
    this.feeForm.markAsPristine();
    this.scheduleForm.markAsPristine();
  }

  private ensureCurrentOptions(batch: Batch): void {
    if (!this.courses().some((course) => course.id === batch.course.id)) {
      this.courses.update((courses) => [...courses, {
        id: batch.course.id,
        name: batch.course.name,
        shortCode: batch.course.shortCode,
        description: null,
        status: 'INACTIVE',
        batchCount: 0,
        createdAt: '',
        updatedAt: '',
        version: 0,
      }]);
    }
    if (!this.branches().some((branch) => branch.id === batch.branch.id)) {
      this.branches.update((branches) => [...branches, {
        id: batch.branch.id,
        code: batch.branch.code,
        name: batch.branch.name,
        address: null,
        contactNumber: null,
        status: 'INACTIVE',
        defaultBranch: false,
        createdAt: '',
        updatedAt: '',
        version: 0,
      }]);
    }
  }

  private feePlanFormValue(feePlan: FeePlan) {
    return {
      registrationFee: feePlan.registrationFee,
      courseFee: feePlan.courseFee,
      examinationFee: feePlan.examinationFee,
      monthlyDueDay: feePlan.monthlyDueDay,
      examinationDueDate: toLocalDate(feePlan.examinationDueDate),
      currencyCode: feePlan.currencyCode,
    };
  }

  private applyServerFieldErrors(error: unknown): void {
    const response = error as { error?: { fieldErrors?: Array<{ field: string }> } };
    for (const item of response.error?.fieldErrors ?? []) {
      const controls: Array<AbstractControl | null> = [
        this.generalForm.get(item.field),
        this.feeForm.get(item.field),
        this.scheduleForm.get(item.field),
      ];
      const control = controls.find((candidate): candidate is AbstractControl => Boolean(candidate));
      if (control) {
        control.setErrors({ ...control.errors, server: true });
        control.markAsTouched();
      }
    }
  }

  private deduplicate(values: number[]): number[] {
    return Array.from(new Set(values));
  }

  private feePlanRequest(): FeePlanRequest {
    return {
      registrationFee: this.money(this.feeForm.controls.registrationFee.value),
      courseFee: this.money(this.feeForm.controls.courseFee.value),
      examinationFee: this.money(this.feeForm.controls.examinationFee.value),
      durationMonths: this.generalForm.controls.durationMonths.value,
      monthlyDueDay: this.feeForm.controls.monthlyDueDay.value,
      examinationDueDate: toIsoDate(this.feeForm.controls.examinationDueDate.value),
      currencyCode: this.feeForm.controls.currencyCode.value,
      status: 'ACTIVE',
    };
  }

  private installmentDueDate(startDate: string, dueDay: number, installmentNumber: number): string {
    const start = new Date(`${startDate}T00:00:00`);
    const month = new Date(start.getFullYear(), start.getMonth() + installmentNumber - 1, 1);
    const lastDay = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
    const dueDate = new Date(month.getFullYear(), month.getMonth(), Math.min(dueDay, lastDay));
    if (installmentNumber === 1 && dueDate < start) {
      return startDate;
    }
    return this.formatDate(dueDate);
  }

  private money(value: number): number {
    return Math.round(Number(value || 0) * 100) / 100;
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
