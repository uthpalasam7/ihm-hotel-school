import { DatePipe, SlicePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { DateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { DayMonthYearDateAdapter } from '../shared/day-month-year-date-adapter';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, finalize, forkJoin, interval } from 'rxjs';
import { Batch, BatchLecturer } from '../batches/batch.models';
import { BatchService } from '../batches/batch.service';
import { AuthService } from '../core/auth/auth.service';
import { errorMessage } from '../shared/api-error';
import { ConfirmationDialogComponent, ConfirmationDialogResult } from '../shared/confirmation-dialog.component';
import { DateValue, toIsoDate, toLocalDate } from '../shared/date-value';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import { SchedulePatternDialogComponent } from './schedule-pattern-dialog.component';
import { GenerationOutcome, GenerationPreview, GenerationRequest, GenerationResult, OUTCOME_LABELS, Schedule, WEEKDAYS } from './schedule.models';
import { ScheduleService } from './schedule.service';
import { generationRange } from './schedule-validators';

@Component({
  selector: 'app-batch-schedule',
  imports: [DatePipe, SlicePipe, ReactiveFormsModule, MatButtonModule, MatCardModule, MatDatepickerModule,
    MatFormFieldModule, MatInputModule, MatPaginatorModule, MatTableModule, RouterLink,
    PageHeaderComponent, PageStateComponent, StatusChipComponent],
  providers: [{ provide: DateAdapter, useClass: DayMonthYearDateAdapter }, { provide: MAT_DATE_LOCALE, useValue: 'en-GB' }],
  templateUrl: './batch-schedule.component.html',
  styleUrl: './batch-schedule.component.scss',
})
export class BatchScheduleComponent implements OnInit {
  private readonly api = inject(ScheduleService);
  private readonly batches = inject(BatchService);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  readonly canManage = inject(AuthService).hasAnyRole(['SUPER_ADMIN', 'ADMIN']);
  private batchId = 0;
  private readRequest?: Subscription;
  private previewRequest?: Subscription;
  private previewInput: GenerationRequest | null = null;
  readonly batch = signal<Batch | null>(null);
  readonly patterns = signal<Schedule[]>([]);
  readonly lecturers = signal<BatchLecturer[]>([]);
  readonly loading = signal(false);
  readonly working = signal(false);
  readonly previewLoading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly previewError = signal<string | null>(null);
  readonly result = signal<GenerationResult | null>(null);
  readonly preview = signal<GenerationPreview | null>(null);
  readonly excludedDates = signal<string[]>([]);
  readonly page = signal(0);
  readonly size = signal(20);
  readonly total = signal(0);
  readonly previewPage = signal(0);
  readonly previewSize = signal(20);
  readonly days = WEEKDAYS;
  outcomeLabel(outcome: GenerationOutcome): string { return OUTCOME_LABELS[outcome]; }
  readonly patternColumns = ['day', 'time', 'lecturer', 'classroom', 'status', 'actions'];
  readonly previewColumns = ['date', 'time', 'lecturer', 'classroom', 'outcome'];
  readonly busy = computed(() => this.loading() || this.working() || this.previewLoading());
  readonly writable = computed(() => this.canManage && this.batch()?.scheduleMode === 'REGULAR'
    && ['ACTIVE', 'UPCOMING'].includes(this.batch()?.status ?? ''));
  private readonly now = signal(Date.now());
  readonly expired = computed(() => !!this.preview() && Date.parse(this.preview()!.expiresAt) <= this.now());
  readonly previewRows = computed(() => this.preview()?.sessions.slice(this.previewPage() * this.previewSize(), (this.previewPage() + 1) * this.previewSize()) ?? []);
  readonly minDate = computed(() => toLocalDate(this.batch()?.startDate ?? null));
  readonly maxDate = computed(() => toLocalDate(this.batch()?.endDate ?? null));
  readonly range = new FormGroup({
    fromDate: new FormControl<DateValue>(null, Validators.required),
    toDate: new FormControl<DateValue>(null, Validators.required),
  }, { validators: generationRange });
  readonly exclusion = new FormControl<DateValue>(null);

  ngOnInit(): void {
    this.range.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => { this.invalidatePreview(); this.result.set(null); });
    interval(1000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.now.set(Date.now()));
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.readRequest?.unsubscribe();
      this.invalidatePreview(); this.result.set(null); this.batch.set(null); this.excludedDates.set([]); this.actionError.set(null);
      this.batchId = Number(params.get('id'));
      if (!this.canManage) { this.loadError.set('You do not have permission to manage weekly schedules.'); return; }
      if (!Number.isInteger(this.batchId) || this.batchId < 1) { this.loadError.set('Invalid batch. Return to Batches and select a record.'); return; }
      this.load(0, true);
    });
    this.destroyRef.onDestroy(() => { this.readRequest?.unsubscribe(); this.previewRequest?.unsubscribe(); });
  }

  hasUnsavedChanges(): boolean { return this.working(); }

  load(page = this.page(), resetRange = false): void {
    if (!this.canManage || this.batchId < 1) return;
    this.readRequest?.unsubscribe(); this.invalidatePreview(); this.loading.set(true); this.loadError.set(null); this.page.set(page);
    this.readRequest = forkJoin({ batch: this.batches.get(this.batchId), patterns: this.api.list(this.batchId, page, this.size()), lecturers: this.batches.lecturers(this.batchId) })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
        next: data => {
          this.batch.set(data.batch); this.patterns.set(data.patterns.content); this.total.set(data.patterns.totalElements);
          this.page.set(data.patterns.page);
          this.lecturers.set(data.lecturers.filter((l, i, all) => l.status === 'ACTIVE'
            && l.assignmentStartDate <= data.batch.endDate && (!l.assignmentEndDate || l.assignmentEndDate >= data.batch.startDate)
            && all.findIndex(other => other.lecturerUserId === l.lecturerUserId && other.status === 'ACTIVE') === i));
          if (resetRange || !this.range.controls.fromDate.value) {
            const end = toLocalDate(data.batch.startDate)!; end.setDate(end.getDate() + 365);
            this.range.setValue({ fromDate: toLocalDate(data.batch.startDate), toDate: toLocalDate(toIsoDate(end) < data.batch.endDate ? toIsoDate(end) : data.batch.endDate) });
          }
        }, error: error => this.loadError.set(errorMessage(error)),
      });
  }

  changePage(event: PageEvent): void { this.size.set(event.pageSize); this.load(event.pageIndex); }
  changePreviewPage(event: PageEvent): void { this.previewSize.set(event.pageSize); this.previewPage.set(event.pageIndex); }

  edit(pattern?: Schedule): void {
    if (!this.writable() || this.busy()) return;
    this.invalidatePreview(); this.actionError.set(null); this.result.set(null);
    const batchId = this.batchId;
    this.working.set(true);
    this.dialog.open(SchedulePatternDialogComponent, { data: { batchId, batchNumber: this.batch()!.batchNumber,
      lecturers: this.lecturers(), schedule: pattern }, width: '44rem', maxWidth: 'calc(100vw - 2rem)' })
      .afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(saved => {
        this.working.set(false);
        if (saved && batchId === this.batchId) { this.notifications.success('Weekly pattern saved'); this.load(0); }
      });
  }

  deactivate(pattern: Schedule): void {
    if (!this.canManage || this.busy() || pattern.status !== 'ACTIVE') return;
    this.invalidatePreview(); this.actionError.set(null); this.result.set(null);
    const batchId = this.batchId;
    this.working.set(true);
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(ConfirmationDialogComponent, {
      data: { title: 'Deactivate weekly pattern?', message: `${this.days[pattern.dayOfWeek - 1]}, ${pattern.startTime.slice(0, 5)}–${pattern.endTime.slice(0, 5)} will stop contributing to future generation. Saved sessions stay unchanged.`, confirmLabel: 'Deactivate pattern' },
      width: '34rem', maxWidth: 'calc(100vw - 2rem)',
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(answer => {
      if (!answer?.confirmed || batchId !== this.batchId) { this.working.set(false); return; }
      this.api.deactivate(batchId, pattern).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.working.set(false))).subscribe({
        next: () => { if (batchId === this.batchId) { this.notifications.success('Weekly pattern deactivated'); this.load(); } },
        error: error => { if (batchId === this.batchId) this.actionError.set(errorMessage(error)); },
      });
    });
  }

  addExclusion(): void {
    if (this.working()) return;
    const date = toIsoDate(this.exclusion.value), input = this.range.getRawValue();
    if (this.exclusion.invalid || !date || date < toIsoDate(input.fromDate) || date > toIsoDate(input.toDate)) {
      this.previewError.set('Choose an excluded date within the preview range.'); return;
    }
    this.invalidatePreview(); this.result.set(null);
    this.excludedDates.update(dates => [...new Set([...dates, date])].sort()); this.exclusion.reset();
  }
  removeExclusion(date: string): void {
    if (this.working()) return;
    this.invalidatePreview(); this.result.set(null); this.excludedDates.update(dates => dates.filter(d => d !== date));
  }
  invalidatePreview(): void {
    this.previewRequest?.unsubscribe(); this.previewLoading.set(false); this.preview.set(null); this.previewInput = null; this.previewError.set(null);
  }

  createPreview(): void {
    if (!this.writable() || this.busy()) return;
    this.range.markAllAsTouched(); this.invalidatePreview(); this.result.set(null);
    if (this.range.invalid) { this.previewError.set('Choose a valid date range of at most 366 days.'); return; }
    const input = this.range.getRawValue();
    const request: GenerationRequest = { fromDate: toIsoDate(input.fromDate), toDate: toIsoDate(input.toDate), excludeDates: [...this.excludedDates()] };
    const batch = this.batch()!;
    if (request.fromDate < batch.startDate || request.toDate > batch.endDate || request.excludeDates.some(d => d < request.fromDate || d > request.toDate)) {
      this.previewError.set('Keep the range within the batch dates and remove exclusions outside the range.'); return;
    }
    this.previewLoading.set(true);
    this.previewRequest = this.api.preview(batch.id, request).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.previewLoading.set(false))).subscribe({
      next: preview => { this.previewInput = request; this.preview.set(preview); this.previewPage.set(0); this.now.set(Date.now()); },
      error: error => this.previewError.set(errorMessage(error)),
    });
  }

  generate(): void {
    const preview = this.preview(), input = this.previewInput, batchId = this.batchId;
    if (!this.writable() || this.busy() || !preview || !input || preview.conflictCount || !preview.createCount) return;
    if (Date.parse(preview.expiresAt) <= Date.now()) { this.invalidatePreview(); this.previewError.set('This preview expired. Preview the sessions again.'); return; }
    this.working.set(true); this.range.disable({ emitEvent: false }); this.exclusion.disable({ emitEvent: false });
    const unlock = () => { this.working.set(false); this.range.enable({ emitEvent: false }); this.exclusion.enable({ emitEvent: false }); };
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(ConfirmationDialogComponent, {
      data: { title: `Generate ${preview.createCount} sessions?`, message: `${this.batch()!.batchNumber}: ${preview.createCount} new sessions will be saved; ${preview.skipCount} existing sessions will be kept.`, confirmLabel: 'Generate sessions' },
      width: '34rem', maxWidth: 'calc(100vw - 2rem)',
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(answer => {
      if (!answer?.confirmed || batchId !== this.batchId || this.preview() !== preview) { unlock(); return; }
      if (Date.parse(preview.expiresAt) <= Date.now()) { unlock(); this.invalidatePreview(); this.previewError.set('This preview expired. Preview the sessions again.'); return; }
      this.api.generate(batchId, input, preview.previewToken).pipe(takeUntilDestroyed(this.destroyRef), finalize(unlock)).subscribe({
        next: result => { if (batchId === this.batchId) { this.invalidatePreview(); this.result.set(result); this.notifications.success(`${result.createdCount} sessions generated`); } },
        error: error => { if (batchId === this.batchId) { this.invalidatePreview(); this.previewError.set(`${errorMessage(error)} Preview again before retrying.`); } },
      });
    });
  }
}
