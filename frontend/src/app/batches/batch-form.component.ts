import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of, switchMap } from 'rxjs';
import { Branch } from '../branches/branch.models';
import { BranchService } from '../branches/branch.service';
import { Course } from '../courses/course.models';
import { CourseService } from '../courses/course.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { errorMessage, fieldError } from '../shared/api-error';
import { UserAccount } from '../users/user.models';
import { UserService } from '../users/user.service';
import { Batch, BatchLecturer, FeePlanRequest, PreviewCharge } from './batch.models';
import { BatchService } from './batch.service';

@Component({
  selector: 'app-batch-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './batch-form.component.html',
  styleUrl: './batch-form.component.scss',
})
export class BatchFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly batchService = inject(BatchService);
  private readonly courseService = inject(CourseService);
  private readonly branchService = inject(BranchService);
  private readonly userService = inject(UserService);
  private readonly activeBranchService = inject(ActiveBranchService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverError = signal<unknown>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly step = signal(1);
  protected readonly courses = signal<Course[]>([]);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly lecturers = signal<UserAccount[]>([]);
  protected readonly assignments = signal<BatchLecturer[]>([]);
  protected readonly lecturerSearch = signal('');
  protected selectedLecturerIds: number[] = [];

  protected readonly generalForm = this.fb.nonNullable.group({
    courseId: [0, [Validators.required, Validators.min(1)]],
    branchId: [0, [Validators.required, Validators.min(1)]],
    batchNumber: ['', [Validators.required, Validators.maxLength(60)]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    durationMonths: [6, [Validators.required, Validators.min(1)]],
    status: ['UPCOMING', [Validators.required]],
    remarks: [''],
  });

  protected readonly feeForm = this.fb.nonNullable.group({
    registrationFee: [0, [Validators.required, Validators.min(0)]],
    courseFee: [0, [Validators.required, Validators.min(0)]],
    examinationFee: [0, [Validators.required, Validators.min(0)]],
    monthlyDueDay: [10, [Validators.required, Validators.min(1), Validators.max(31)]],
    examinationDueDate: ['', [Validators.required]],
    currencyCode: ['LKR', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
  });

  protected readonly scheduleForm = this.fb.nonNullable.group({
    scheduleMode: ['REGULAR', [Validators.required]],
  });

  protected readonly lecturerForm = this.fb.nonNullable.group({
    assignmentStartDate: ['', [Validators.required]],
    assignmentEndDate: [''],
  });

  ngOnInit(): void {
    const activeBranch = this.activeBranchService.activeBranch();
    this.generalForm.patchValue({ branchId: activeBranch?.id ?? 0 });
    this.loading.set(true);
    forkJoin({
      courses: this.courseService.list({ status: 'ACTIVE', size: 100 }),
      branches: this.branchService.list({ status: 'ACTIVE', size: 100 }),
    }).subscribe({
      next: ({ courses, branches }) => {
        this.courses.set(courses.content);
        this.branches.set(branches.content);
        if (!this.generalForm.controls.courseId.value && courses.content[0]) {
          this.generalForm.patchValue({ courseId: courses.content[0].id });
        }
        if (!this.generalForm.controls.branchId.value && branches.content[0]) {
          this.generalForm.patchValue({ branchId: branches.content[0].id });
        }
        this.loadForEdit();
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected setStep(step: number): void {
    if (step >= 1 && step <= 5) {
      this.step.set(step);
    }
  }

  protected next(): void {
    this.setStep(this.step() + 1);
  }

  protected previous(): void {
    this.setStep(this.step() - 1);
  }

  protected filteredLecturers(): UserAccount[] {
    const query = this.lecturerSearch().trim().toLowerCase();
    if (!query) {
      return this.lecturers();
    }
    return this.lecturers().filter((lecturer) =>
      lecturer.fullName.toLowerCase().includes(query) || lecturer.username.toLowerCase().includes(query));
  }

  protected selectedLecturerSummaries(): Array<{ id: number; fullName: string; username: string }> {
    const lecturersById = new Map(this.lecturers().map((lecturer) => [lecturer.id, lecturer]));
    const assignmentsByLecturerId = new Map(this.assignments()
      .filter((assignment) => assignment.status === 'ACTIVE')
      .map((assignment) => [assignment.lecturerUserId, assignment]));
    return this.selectedLecturerIds.map((id) => {
      const lecturer = lecturersById.get(id);
      if (lecturer) {
        return { id, fullName: lecturer.fullName, username: lecturer.username };
      }
      const assignment = assignmentsByLecturerId.get(id);
      return {
        id,
        fullName: assignment?.lecturerFullName ?? `Lecturer ${id}`,
        username: assignment?.lecturerUsername ?? 'selected',
      };
    });
  }

  protected isLecturerSelected(lecturerId: number): boolean {
    return this.selectedLecturerIds.includes(lecturerId);
  }

  protected toggleLecturer(lecturerId: number, selected: boolean): void {
    if (selected) {
      this.selectedLecturerIds = this.deduplicate([...this.selectedLecturerIds, lecturerId]);
      return;
    }
    this.removeSelectedLecturer(lecturerId);
  }

  protected removeSelectedLecturer(lecturerId: number): void {
    this.selectedLecturerIds = this.selectedLecturerIds.filter((id) => id !== lecturerId);
  }

  protected loadLecturers(): void {
    const branchId = this.generalForm.controls.branchId.value;
    if (!branchId) {
      this.lecturers.set([]);
      return;
    }
    this.userService.list({ role: 'LECTURER', branchId, status: 'ACTIVE', size: 100 }).subscribe({
      next: (page) => {
        this.lecturers.set(page.content);
        const validLecturerIds = new Set(page.content.map((lecturer) => lecturer.id));
        this.selectedLecturerIds = this.selectedLecturerIds.filter((id) => validLecturerIds.has(id));
      },
      error: (error) => this.error.set(errorMessage(error)),
    });
  }

  protected branchChanged(): void {
    this.selectedLecturerIds = [];
    this.assignments.set([]);
    this.lecturerSearch.set('');
    this.loadLecturers();
  }

  protected previewCharges(): PreviewCharge[] {
    const startDate = this.generalForm.controls.startDate.value;
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
      dueDate: this.feeForm.controls.examinationDueDate.value,
      amount: this.money(this.feeForm.controls.examinationFee.value),
    });
    return charges;
  }

  protected save(): void {
    this.generalForm.markAllAsTouched();
    this.feeForm.markAllAsTouched();
    this.scheduleForm.markAllAsTouched();
    if (this.selectedLecturerIds.length > 0) {
      this.lecturerForm.markAllAsTouched();
    }
    if (
      this.generalForm.invalid
      || this.feeForm.invalid
      || this.scheduleForm.invalid
      || (this.selectedLecturerIds.length > 0 && this.lecturerForm.invalid)
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
      startDate: this.generalForm.controls.startDate.value,
      endDate: this.generalForm.controls.endDate.value,
      durationMonths: this.generalForm.controls.durationMonths.value,
      scheduleMode: this.scheduleForm.controls.scheduleMode.value as 'REGULAR' | 'MANUAL',
      status: this.generalForm.controls.status.value as 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED',
      remarks: this.generalForm.controls.remarks.value || null,
    };
    const id = this.editingId();
    const batchAction = id ? this.batchService.update(id, batchRequest) : this.batchService.create(batchRequest);
    batchAction.pipe(
      switchMap((batch) => this.batchService.saveFeePlan(batch.id, this.feePlanRequest()).pipe(switchMap(() => this.saveLecturers(batch)))),
    ).subscribe({
      next: () => this.router.navigateByUrl('/batches'),
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

  protected syncAssignmentStartDate(): void {
    const startDate = this.generalForm.controls.startDate.value;
    if (startDate && !this.lecturerForm.controls.assignmentStartDate.value) {
      this.lecturerForm.patchValue({ assignmentStartDate: startDate });
    }
  }

  private loadForEdit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.loadLecturers();
      this.loading.set(false);
      return;
    }
    this.editingId.set(id);
    this.batchService.get(id).subscribe({
      next: (batch) => {
        this.generalForm.patchValue({
          courseId: batch.course.id,
          branchId: batch.branch.id,
          batchNumber: batch.batchNumber,
          startDate: batch.startDate,
          endDate: batch.endDate,
          durationMonths: batch.durationMonths,
          status: batch.status,
          remarks: batch.remarks ?? '',
        });
        this.scheduleForm.patchValue({ scheduleMode: batch.scheduleMode });
        this.lecturerForm.patchValue({ assignmentStartDate: batch.startDate });
        this.loadLecturers();
        this.batchService.getFeePlan(id).pipe(catchError(() => of(null))).subscribe({
          next: (feePlan) => {
            if (feePlan) {
              this.feeForm.patchValue(feePlan);
            }
          },
        });
        this.batchService.lecturers(id).subscribe({
          next: (assignments) => {
            this.assignments.set(assignments);
            this.selectedLecturerIds = this.deduplicate(assignments
              .filter((assignment) => assignment.status === 'ACTIVE')
              .map((assignment) => assignment.lecturerUserId));
          },
        });
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
    const assignmentStartDate = this.lecturerForm.controls.assignmentStartDate.value || this.generalForm.controls.startDate.value;
    const assignmentEndDate = this.lecturerForm.controls.assignmentEndDate.value || null;
    return this.batchService.syncLecturers(batch.id, {
      lecturerUserIds: selectedLecturerIds,
      assignmentStartDate,
      assignmentEndDate,
    });
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
      examinationDueDate: this.feeForm.controls.examinationDueDate.value,
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
