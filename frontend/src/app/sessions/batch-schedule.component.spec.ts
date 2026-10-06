import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Batch } from '../batches/batch.models';
import { BatchService } from '../batches/batch.service';
import { AuthService } from '../core/auth/auth.service';
import { NotificationService } from '../shared/notification.service';
import { BatchScheduleComponent } from './batch-schedule.component';
import { GenerationPreview, Schedule } from './schedule.models';
import { ScheduleService } from './schedule.service';

const batch: Batch = { id: 7, course: { id: 1, name: 'Cookery', shortCode: 'CK' }, branch: { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }, batchNumber: '2026/CK01', startDate: '2026-07-01', endDate: '2026-07-31', durationMonths: 1, scheduleMode: 'REGULAR', status: 'ACTIVE', registrationSequence: 0, lecturerCount: 0, studentCount: 0, feePlanConfigured: true, createdAt: '', updatedAt: '', version: 0 };
const pattern: Schedule = { id: 2, batchId: 7, dayOfWeek: 3, startTime: '09:00:00', endTime: '13:00:00', defaultLecturerUserId: null, defaultLecturerName: null, classroom: 'Kitchen 1', status: 'ACTIVE', version: 4 };
const plan: GenerationPreview = { previewToken: 'signed-preview', expiresAt: '2099-01-01T00:00:00Z', createCount: 1, skipCount: 0, conflictCount: 0, sessions: [{ scheduleId: 2, sessionDate: '2026-07-01', startTime: '09:00', endTime: '13:00', lecturerUserId: null, lecturerName: null, classroom: 'Kitchen 1', outcome: 'CREATE', existingSessionId: null, message: null }] };

describe('BatchScheduleComponent', () => {
  let fixture: ComponentFixture<BatchScheduleComponent>; let c: BatchScheduleComponent;
  let api: { list: ReturnType<typeof vi.fn>; preview: ReturnType<typeof vi.fn>; generate: ReturnType<typeof vi.fn>; deactivate: ReturnType<typeof vi.fn> };
  let batches: { get: ReturnType<typeof vi.fn>; lecturers: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> }; let roles: ReturnType<typeof vi.fn>;
  let route: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  beforeEach(async () => {
    api = { list: vi.fn(() => of({ content: [pattern], page: 0, size: 20, totalElements: 1, totalPages: 1 })), preview: vi.fn(() => of(plan)), generate: vi.fn(() => of({ createdCount: 1, skippedCount: 0, createdSessionIds: [100] })), deactivate: vi.fn(() => of(null)) };
    batches = { get: vi.fn(() => of(batch)), lecturers: vi.fn(() => of([])) };
    roles = vi.fn(() => true); route = new BehaviorSubject(convertToParamMap({ id: 7 }));
    dialog = { open: vi.fn(() => ({ afterClosed: () => of({ confirmed: true, reason: '' }) })) };
    await TestBed.configureTestingModule({ imports: [BatchScheduleComponent], providers: [provideRouter([]), provideNativeDateAdapter(),
      { provide: ActivatedRoute, useValue: { paramMap: route } }, { provide: ScheduleService, useValue: api }, { provide: BatchService, useValue: batches },
      { provide: AuthService, useValue: { hasAnyRole: roles } }, { provide: MatDialog, useValue: dialog }, { provide: NotificationService, useValue: { success: vi.fn() } },
    ] }).compileComponents();
  });
  async function start() { fixture = TestBed.createComponent(BatchScheduleComponent); c = fixture.componentInstance; fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges(); }
  afterEach(() => fixture?.destroy());
  it('renders a scoped Material grid and mobile cards with page defaults', async () => {
    await start(); expect(fixture.nativeElement.textContent).toContain('2026/CK01');
    expect(fixture.nativeElement.querySelector('table[mat-table]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.ihm-mobile-card__details')).toBeTruthy();
    expect(api.list).toHaveBeenCalledWith(7, 0, 20);
    c.changePage({ pageIndex: 1, pageSize: 50, length: 100 }); expect(api.list).toHaveBeenLastCalledWith(7, 1, 50);
  });
  it('previews local calendar dates and deduplicates holiday exclusions', async () => {
    await start(); c.exclusion.setValue(new Date(2026, 6, 8)); c.addExclusion(); c.exclusion.setValue(new Date(2026, 6, 8)); c.addExclusion(); c.createPreview();
    expect(api.preview).toHaveBeenCalledWith(7, { fromDate: '2026-07-01', toDate: '2026-07-31', excludeDates: ['2026-07-08'] });
    expect(api.generate).not.toHaveBeenCalled(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('New session');
  });
  it('blocks inverted, out-of-batch and excessive ranges and out-of-range holidays', async () => {
    await start(); c.range.setValue({ fromDate: new Date(2026, 6, 20), toDate: new Date(2026, 6, 1) }); c.createPreview();
    c.range.setValue({ fromDate: new Date(2026, 5, 30), toDate: new Date(2026, 6, 1) }); c.createPreview();
    c.range.setValue({ fromDate: new Date(2026, 6, 1), toDate: new Date(2027, 6, 2) }); c.createPreview();
    expect(api.preview).not.toHaveBeenCalled();
    c.range.setValue({ fromDate: new Date(2026, 6, 1), toDate: new Date(2026, 6, 31) }); c.exclusion.setValue(new Date(2026, 7, 1)); c.addExclusion(); expect(c.excludedDates()).toEqual([]);
    c.exclusion.setValue(new Date(2026, 6, 20)); c.addExclusion(); c.range.controls.toDate.setValue(new Date(2026, 6, 10)); c.createPreview(); expect(api.preview).not.toHaveBeenCalled();
  });
  it('invalidates a preview when range, exclusions or pattern editing changes', async () => {
    await start(); c.createPreview(); c.range.controls.toDate.setValue(new Date(2026, 6, 20)); expect(c.preview()).toBeNull();
    c.createPreview(); c.exclusion.setValue(new Date(2026, 6, 8)); c.addExclusion(); expect(c.preview()).toBeNull();
    c.createPreview(); c.removeExclusion('2026-07-08'); expect(c.preview()).toBeNull();
    c.createPreview(); dialog.open.mockReturnValue({ afterClosed: () => of(false) }); c.edit(pattern); expect(c.preview()).toBeNull(); expect(api.generate).not.toHaveBeenCalled();
  });
  it('cancels an obsolete preview request instead of rendering its late response', async () => {
    await start(); const pending = new Subject<GenerationPreview>(); api.preview.mockReturnValue(pending);
    c.createPreview(); c.range.controls.toDate.setValue(new Date(2026, 6, 20)); pending.next(plan); pending.complete(); expect(c.preview()).toBeNull(); expect(c.previewLoading()).toBe(false);
  });
  it('sends only the reviewed request after explicit confirmation and shows the result', async () => {
    await start(); c.createPreview(); c.generate(); expect(dialog.open).toHaveBeenCalled();
    expect(api.generate).toHaveBeenCalledWith(7, { fromDate: '2026-07-01', toDate: '2026-07-31', excludeDates: [] }, 'signed-preview');
    expect(c.preview()).toBeNull(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('1 sessions generated');
  });
  it('does not generate after confirmation is cancelled', async () => {
    await start(); c.createPreview(); dialog.open.mockReturnValue({ afterClosed: () => of(undefined) }); c.generate();
    expect(api.generate).not.toHaveBeenCalled(); expect(c.range.enabled).toBe(true); expect(c.working()).toBe(false);
  });
  it('blocks conflicting, empty and expired previews', async () => {
    await start(); for (const overrides of [{ conflictCount: 1 }, { createCount: 0 }, { expiresAt: '2000-01-01T00:00:00Z' }]) {
      api.preview.mockReturnValue(of({ ...plan, ...overrides })); c.createPreview(); c.generate();
    }
    expect(api.generate).not.toHaveBeenCalled();
  });
  it('rechecks expiry after the confirmation dialog closes', async () => {
    await start(); const confirmation = new Subject<{ confirmed: true }>(); dialog.open.mockReturnValue({ afterClosed: () => confirmation });
    c.createPreview(); c.generate(); const now = vi.spyOn(Date, 'now').mockReturnValue(Date.parse('2100-01-01'));
    confirmation.next({ confirmed: true }); confirmation.complete(); now.mockRestore(); expect(api.generate).not.toHaveBeenCalled(); expect(c.previewError()).toContain('expired');
  });
  it('prevents double submission and freezes the range during generation', async () => {
    await start(); const pending = new Subject(); api.generate.mockReturnValue(pending); c.createPreview(); c.generate(); c.generate();
    expect(api.generate).toHaveBeenCalledTimes(1); expect(c.range.disabled).toBe(true); expect(c.hasUnsavedChanges()).toBe(true); pending.complete(); expect(c.range.enabled).toBe(true);
  });
  it('clears stale confirmation after a server conflict and keeps useful errors', async () => {
    await start(); api.generate.mockReturnValue(throwError(() => ({ error: { message: 'Session generation has conflicts' } })));
    c.createPreview(); c.generate(); expect(c.preview()).toBeNull(); expect(c.previewError()).toContain('Preview again'); expect(c.working()).toBe(false);
    api.preview.mockReturnValue(throwError(() => ({ error: { message: 'Configure an active weekly pattern' } }))); c.createPreview(); expect(c.previewError()).toContain('active weekly pattern');
  });
  it('deactivates only after confirmation and supplies the current version', async () => {
    await start(); dialog.open.mockReturnValueOnce({ afterClosed: () => of(undefined) }); c.deactivate(pattern); expect(api.deactivate).not.toHaveBeenCalled();
    c.deactivate(pattern); expect(api.deactivate).toHaveBeenCalledWith(7, pattern);
    api.deactivate.mockReturnValue(throwError(() => ({ error: { message: 'Weekly pattern changed' } }))); c.deactivate(pattern); expect(c.actionError()).toBe('Weekly pattern changed');
  });
  it('does not fetch or expose controls to a lecturer', async () => {
    roles.mockReturnValue(false); await start(); expect(batches.get).not.toHaveBeenCalled(); expect(fixture.nativeElement.textContent).not.toContain('Add pattern'); c.createPreview(); expect(api.preview).not.toHaveBeenCalled();
  });
  it('keeps manual and retired batches read-only for generation', async () => {
    batches.get.mockReturnValue(of({ ...batch, scheduleMode: 'MANUAL' })); await start(); expect(c.writable()).toBe(false); c.createPreview(); expect(api.preview).not.toHaveBeenCalled();
    batches.get.mockReturnValue(of({ ...batch, status: 'COMPLETED' })); c.load(); expect(c.writable()).toBe(false); c.generate(); expect(api.generate).not.toHaveBeenCalled();
  });
  it('resets review state when navigating to another batch and can retry load failures', async () => {
    await start(); c.createPreview(); route.next(convertToParamMap({ id: 8 })); expect(c.preview()).toBeNull(); expect(batches.get).toHaveBeenLastCalledWith(8);
    batches.get.mockReturnValueOnce(throwError(() => ({ error: { message: 'Access denied' } }))); c.load(); expect(c.loadError()).toBe('Access denied'); c.load(); expect(c.loadError()).toBeNull();
  });
  it('paginates the bounded preview snapshot without refetching a partial server list', async () => {
    await start(); api.preview.mockReturnValue(of({ ...plan, sessions: Array.from({ length: 25 }, (_, i) => ({ ...plan.sessions[0], scheduleId: i + 1 })) })); c.createPreview();
    expect(c.previewRows().length).toBe(20); c.changePreviewPage({ pageIndex: 1, pageSize: 20, length: 25 }); expect(c.previewRows().length).toBe(5); expect(api.preview).toHaveBeenCalledTimes(1);
  });
});
