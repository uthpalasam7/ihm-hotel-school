import { DatePipe, SlicePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { DateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, catchError, debounceTime, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { Batch, BatchLecturer } from '../batches/batch.models';
import { BatchService } from '../batches/batch.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { errorMessage } from '../shared/api-error';
import { ConfirmationDialogComponent, ConfirmationDialogResult } from '../shared/confirmation-dialog.component';
import { DateValue, toIsoDate } from '../shared/date-value';
import { DayMonthYearDateAdapter } from '../shared/day-month-year-date-adapter';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import { ClassSession, SessionFilters, SessionRescheduleResponse, SessionStatus } from './session.models';
import { SessionService } from './session.service';
import { SessionEditorDialogComponent, SessionEditorResult } from './session-editor-dialog.component';

const CALENDAR_LIMIT = 500;
type ViewMode = 'calendar' | 'list';

function monthStart(date: Date): Date { return new Date(date.getFullYear(), date.getMonth(), 1); }
function monthEnd(date: Date): Date { return new Date(date.getFullYear(), date.getMonth() + 1, 0); }
function calendarStart(date: Date): Date {
  const first = monthStart(date);
  first.setDate(first.getDate() - (first.getDay() + 6) % 7);
  return first;
}
function addDays(date: Date, days: number): Date {
  const result = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  result.setDate(result.getDate() + days);
  return result;
}

@Component({
  selector: 'app-session-list',
  imports: [DatePipe, SlicePipe, ReactiveFormsModule, MatAutocompleteModule, MatButtonModule, MatCardModule,
    MatDatepickerModule, MatFormFieldModule, MatInputModule, MatPaginatorModule, MatSelectModule, MatTableModule,
    PageHeaderComponent, PageStateComponent, StatusChipComponent, RouterLink],
  providers: [{ provide: DateAdapter, useClass: DayMonthYearDateAdapter }, { provide: MAT_DATE_LOCALE, useValue: 'en-GB' }],
  templateUrl: './session-list.component.html',
  styleUrl: './session-list.component.scss',
})
export class SessionListComponent implements OnInit {
  private readonly api = inject(SessionService);
  private readonly batches = inject(BatchService);
  private readonly branches = inject(ActiveBranchService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readRequest?: Subscription;
  private detailRequest?: Subscription;
  private initialized = false;
  private lastBranchId = this.branches.activeBranchId();
  protected scopeBatchId: number | null = null;
  readonly view = signal<ViewMode>('calendar');
  readonly month = signal(monthStart(new Date()));
  readonly rows = signal<ClassSession[]>([]);
  readonly selected = signal<ClassSession | null>(null);
  readonly batch = signal<Batch | null>(null);
  readonly lecturers = signal<BatchLecturer[]>([]);
  readonly batchOptions = signal<Batch[]>([]);
  readonly loading = signal(false);
  readonly detailLoading = signal(false);
  readonly batchLoading = signal(false);
  readonly working = signal(false);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly tooMany = signal(false);
  readonly page = signal(0);
  readonly size = signal(20);
  readonly total = signal(0);
  readonly columns = ['date', 'batch', 'time', 'lecturer', 'status', 'actions'];
  readonly weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  readonly batchSearch = new FormControl<string | Batch>('', { nonNullable: true });
  readonly lecturerId = new FormControl<number | null>({ value: null, disabled: true });
  readonly status = new FormControl<SessionStatus | ''>('', { nonNullable: true });
  readonly dateFrom = new FormControl<DateValue>(monthStart(new Date()));
  readonly dateTo = new FormControl<DateValue>(monthEnd(new Date()));
  private applied: Omit<SessionFilters, 'page' | 'size' | 'dateFrom' | 'dateTo'> = {};
  private appliedDates = { from: toIsoDate(this.dateFrom.value), to: toIsoDate(this.dateTo.value) };
  readonly days = computed(() => Array.from({ length: 42 }, (_, index) => addDays(calendarStart(this.month()), index)));
  readonly monthLabel = computed(() => this.month().toLocaleDateString('en-GB', { month: 'long', year: 'numeric' }));
  readonly canAdd = computed(() => !!this.batch() && ['UPCOMING', 'ACTIVE'].includes(this.batch()!.status));

  constructor() {
    effect(() => {
      const branchId = this.branches.activeBranchId();
      if (branchId === this.lastBranchId) return;
      this.lastBranchId = branchId;
      if (this.initialized) {
        if (this.batch()?.branch.id === branchId) { this.load(); return; }
        this.selected.set(null);
        if (this.scopeBatchId) {
          this.scopeBatchId = null;
          void this.router.navigate(['/sessions'], { replaceUrl: true });
          return;
        }
        this.scopeBatchId = null; this.batch.set(null); this.lecturers.set([]);
        this.lecturerId.disable({ emitEvent: false });
        this.batchSearch.enable({ emitEvent: false });
        this.batchSearch.setValue('', { emitEvent: false }); this.applied = {}; this.page.set(0); this.selected.set(null); this.load();
      }
    });
  }

  ngOnInit(): void {
    this.batchSearch.valueChanges.pipe(debounceTime(250), switchMap(value => {
      const term = typeof value === 'string' ? value.trim() : '';
      return term ? this.batches.list({ search: term, page: 0, size: 20 }).pipe(
        map(result => result.content), catchError(() => of([] as Batch[]))) : of([] as Batch[]);
    }), takeUntilDestroyed(this.destroyRef)).subscribe(options => this.batchOptions.set(options));
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = Number(params.get('batchId'));
      this.scopeBatchId = Number.isInteger(id) && id > 0 ? id : null;
      if (this.scopeBatchId) this.selectBatchById(this.scopeBatchId);
      else {
        this.batch.set(null); this.lecturers.set([]); this.lecturerId.disable({ emitEvent: false }); this.batchSearch.setValue('', { emitEvent: false });
        this.batchSearch.enable({ emitEvent: false });
        this.applied = {}; this.selected.set(null); this.page.set(0); this.initialized = true; this.load();
      }
    });
    this.destroyRef.onDestroy(() => { this.readRequest?.unsubscribe(); this.detailRequest?.unsubscribe(); });
  }

  displayBatch(value: Batch | string | null): string { return typeof value === 'string' ? value : value?.batchNumber ?? ''; }
  chooseBatch(event: MatAutocompleteSelectedEvent): void { this.setBatch(event.option.value as Batch); }
  private selectBatchById(id: number): void {
    this.batchLoading.set(true);
    forkJoin({ batch: this.batches.get(id), lecturers: this.batches.lecturers(id) })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.batchLoading.set(false))).subscribe({
        next: ({ batch, lecturers }) => {
          this.batch.set(batch); this.lecturers.set(lecturers);
          this.batchSearch.setValue(batch, { emitEvent: false }); this.batchSearch.disable({ emitEvent: false });
          this.lecturerId.enable({ emitEvent: false }); this.lecturerId.setValue(null);
          this.focusBatchDates(batch); this.applied = { batchId: batch.id }; this.initialized = true; this.page.set(0); this.load();
        }, error: error => {
          this.error.set(errorMessage(error)); this.initialized = true;
          if (error.status === 403) this.clearInaccessibleSession();
        },
      });
  }
  private setBatch(batch: Batch | null): void {
    this.batch.set(batch); this.lecturerId.setValue(null); this.lecturers.set([]);
    if (batch) this.lecturerId.enable({ emitEvent: false }); else this.lecturerId.disable({ emitEvent: false });
    if (!batch) return;
    this.focusBatchDates(batch);
    this.batches.lecturers(batch.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: lecturers => { if (this.batch()?.id === batch.id) this.lecturers.set(lecturers); },
      error: error => {
        this.actionError.set(errorMessage(error));
        if (error.status === 403) this.clearInaccessibleSession();
      },
    });
  }
  private focusBatchDates(batch: Batch): void {
    const today = toIsoDate(new Date());
    const target = today < batch.startDate ? batch.startDate : today > batch.endDate ? batch.endDate : today;
    const [year, month] = target.split('-').map(Number);
    const focused = new Date(year, month - 1, 1);
    this.month.set(focused);
    this.dateFrom.setValue(monthStart(focused)); this.dateTo.setValue(monthEnd(focused));
    this.appliedDates = { from: toIsoDate(this.dateFrom.value), to: toIsoDate(this.dateTo.value) };
  }
  clearBatch(): void {
    if (this.scopeBatchId) return;
    this.setBatch(null); this.batchSearch.setValue('', { emitEvent: false }); this.applyFilters();
  }
  filterLecturers(): BatchLecturer[] {
    return this.lecturers().filter((lecturer, index, all) => lecturer.status === 'ACTIVE'
      && all.findIndex(other => other.lecturerUserId === lecturer.lecturerUserId && other.status === 'ACTIVE') === index);
  }

  applyFilters(): void {
    const value = this.batchSearch.value;
    if (typeof value === 'string' && !value.trim() && !this.scopeBatchId) this.setBatch(null);
    if (typeof value === 'string' && value.trim() && !this.batch()) {
      this.actionError.set('Select a batch from the suggestions, or clear the batch field.'); return;
    }
    if (typeof value === 'string' && this.batch() && value.trim() !== this.batch()!.batchNumber) {
      this.actionError.set('Select a batch from the suggestions.'); return;
    }
    const from = toIsoDate(this.dateFrom.value), to = toIsoDate(this.dateTo.value);
    if (this.view() === 'list' && from && to && from > to) {
      this.actionError.set('The end date must be on or after the start date.'); return;
    }
    this.actionError.set(null); this.selected.set(null); this.page.set(0);
    this.applied = { ...(this.batch() ? { batchId: this.batch()!.id } : {}),
      ...(this.lecturerId.value ? { lecturerId: this.lecturerId.value } : {}),
      ...(this.status.value ? { status: this.status.value } : {}) };
    this.appliedDates = { from, to }; this.load();
  }
  resetFilters(): void {
    this.status.setValue(''); this.lecturerId.setValue(null);
    this.dateFrom.setValue(monthStart(this.month())); this.dateTo.setValue(monthEnd(this.month()));
    if (!this.scopeBatchId) { this.setBatch(null); this.batchSearch.setValue('', { emitEvent: false }); }
    this.applyFilters();
  }
  setView(view: ViewMode): void { if (this.view() !== view) { this.view.set(view); this.page.set(0); this.load(); } }
  changeMonth(offset: number): void {
    this.month.update(date => new Date(date.getFullYear(), date.getMonth() + offset, 1));
    this.selected.set(null);
    if (this.view() === 'calendar') this.load();
  }
  changePage(event: PageEvent): void { this.page.set(event.pageIndex); this.size.set(event.pageSize); this.load(); }
  sessionsOn(date: Date): ClassSession[] { const day = toIsoDate(date); return this.rows().filter(row => row.sessionDate === day); }
  isCurrentMonth(date: Date): boolean { return date.getMonth() === this.month().getMonth(); }
  isToday(date: Date): boolean { return toIsoDate(date) === toIsoDate(new Date()); }

  load(): void {
    this.readRequest?.unsubscribe(); this.loading.set(true); this.error.set(null); this.tooMany.set(false);
    const filters: SessionFilters = { ...this.applied };
    if (this.view() === 'list') {
      if (this.appliedDates.from) filters.dateFrom = this.appliedDates.from;
      if (this.appliedDates.to) filters.dateTo = this.appliedDates.to;
      this.readRequest = this.api.list({ ...filters, page: this.page(), size: this.size() })
        .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
          next: result => { this.rows.set(result.content); this.total.set(result.totalElements); this.page.set(result.page); },
          error: error => {
            this.error.set(errorMessage(error));
            if (error.status === 403) this.clearInaccessibleSession();
          },
        });
      return;
    }
    const days = this.days();
    this.readRequest = this.api.list({ ...filters, dateFrom: toIsoDate(days[0]), dateTo: toIsoDate(days[41]), page: 0, size: 100 })
      .pipe(switchMap(first => {
        if (first.totalElements > CALENDAR_LIMIT) return of({ rows: [] as ClassSession[], tooMany: true });
        const count = Math.ceil(first.totalElements / 100);
        if (count <= 1) return of({ rows: first.content, tooMany: false });
        return forkJoin(Array.from({ length: count - 1 }, (_, index) => this.api.list({ ...filters,
          dateFrom: toIsoDate(days[0]), dateTo: toIsoDate(days[41]), page: index + 1, size: 100 })))
          .pipe(map(rest => {
            const rows = [ ...first.content, ...rest.flatMap(page => page.content) ];
            return { rows: rows.length === first.totalElements ? rows : [], tooMany: rows.length !== first.totalElements };
          }));
      }), takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
        next: result => { this.rows.set(result.rows); this.tooMany.set(result.tooMany); },
        error: error => {
          this.error.set(errorMessage(error));
          if (error.status === 403) this.clearInaccessibleSession();
        },
      });
  }

  open(session: Pick<ClassSession, 'id'>, clearError = true): void {
    this.detailRequest?.unsubscribe(); this.detailLoading.set(true); if (clearError) this.actionError.set(null);
    this.detailRequest = this.api.get(session.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.detailLoading.set(false))).subscribe({
      next: result => this.selected.set(result),
      error: error => {
        this.actionError.set(errorMessage(error));
        if (error.status === 403) this.clearInaccessibleSession();
      },
    });
  }
  closeDetail(): void { this.selected.set(null); }
  canChange(session: ClassSession): boolean { return session.status === 'SCHEDULED' && !session.attendanceSubmittedAt; }
  replacementFor(session: ClassSession): ClassSession | undefined { return this.rows().find(row => row.originalSessionId === session.id); }

  add(): void { if (this.canAdd()) this.edit('create'); }
  edit(mode: 'create' | 'edit' | 'reschedule', session?: ClassSession): void {
    if (this.working() || (mode === 'create' && !this.canAdd()) || (session && !this.canChange(session))) return;
    const batchId = session?.batchId ?? this.batch()?.id;
    if (!batchId) return;
    this.working.set(true); this.actionError.set(null);
    forkJoin({ batch: this.batches.get(batchId), lecturers: this.batches.lecturers(batchId) })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: data => {
          if (!['UPCOMING', 'ACTIVE'].includes(data.batch.status)) {
            this.working.set(false); this.actionError.set('This batch no longer accepts session changes.'); return;
          }
          this.dialog.open<SessionEditorDialogComponent, unknown, SessionEditorResult>(SessionEditorDialogComponent, {
            data: { ...data, mode, session }, width: '46rem', maxWidth: 'calc(100vw - 2rem)',
          }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
            this.working.set(false);
            if (!result) return;
            if ('accessDenied' in result) {
              this.actionError.set('You no longer have access to this batch. The session details were cleared.');
              this.clearInaccessibleSession(); return;
            }
            if ('conflict' in result) {
              this.actionError.set('This session changed or conflicts with another class. The latest details have been reloaded. Review them before retrying.');
              this.load(); if (session) this.open(session, false); return;
            }
            if ('replacement' in result) {
              const rescheduled = result as SessionRescheduleResponse;
              this.selected.set(rescheduled.replacement);
              this.success.set(`Session #${rescheduled.original.id} was rescheduled to #${rescheduled.replacement.id} on ${rescheduled.replacement.sessionDate}.`);
              this.notifications.success('Session rescheduled');
            } else {
              this.selected.set(result);
              this.success.set(mode === 'create' ? 'Class session created.' : 'Class session updated.');
              this.notifications.success(this.success()!);
            }
            this.load();
          });
        }, error: error => {
          this.working.set(false); this.actionError.set(errorMessage(error));
          if (error.status === 403) this.clearInaccessibleSession();
        },
      });
  }
  cancel(session: ClassSession): void {
    if (this.working() || !this.canChange(session)) return;
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(ConfirmationDialogComponent, {
      data: { title: 'Cancel class session?', message: `${session.batchNumber} on ${session.sessionDate} will remain in the history. Attendance cannot be marked for it.`,
        confirmLabel: 'Cancel session', reasonLabel: 'Reason for cancellation', reasonRequired: true },
      width: '34rem', maxWidth: 'calc(100vw - 2rem)',
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(answer => {
      if (!answer?.confirmed) return;
      this.working.set(true); this.actionError.set(null);
      this.api.cancel(session.id, answer.reason, session.version)
        .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.working.set(false))).subscribe({
          next: result => { this.selected.set(result); this.success.set('Class session cancelled.'); this.notifications.success('Class session cancelled'); this.load(); },
          error: error => {
            this.actionError.set(error.status === 409 ? 'This session changed or conflicts with another class. The latest details have been reloaded.' : errorMessage(error));
            if (error.status === 409) { this.load(); this.open(session, false); }
            if (error.status === 403) this.clearInaccessibleSession();
          },
        });
    });
  }

  private clearInaccessibleSession(): void {
    if (!this.actionError()) this.actionError.set('You no longer have access to this batch. Session details were cleared.');
    this.selected.set(null); this.batch.set(null); this.lecturers.set([]); this.batchOptions.set([]);
    this.lecturerId.setValue(null, { emitEvent: false }); this.lecturerId.disable({ emitEvent: false });
    this.batchSearch.enable({ emitEvent: false }); this.batchSearch.setValue('', { emitEvent: false });
    this.applied = {}; this.page.set(0);
    if (this.scopeBatchId) {
      this.scopeBatchId = null;
      void this.router.navigate(['/sessions'], { replaceUrl: true });
    }
  }
}
