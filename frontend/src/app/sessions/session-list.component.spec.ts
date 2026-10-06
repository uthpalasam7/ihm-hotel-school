import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Batch } from '../batches/batch.models';
import { BatchService } from '../batches/batch.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { NotificationService } from '../shared/notification.service';
import { ClassSession } from './session.models';
import { SessionListComponent } from './session-list.component';
import { SessionService } from './session.service';

const batch: Batch = { id: 7, course: { id: 1, name: 'Cookery', shortCode: 'CK' }, branch: { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
  batchNumber: '2026/CK01', startDate: '2026-07-01', endDate: '2026-12-31', durationMonths: 6,
  scheduleMode: 'REGULAR', status: 'ACTIVE', registrationSequence: 0, lecturerCount: 0, studentCount: 0,
  feePlanConfigured: true, createdAt: '', updatedAt: '', version: 0 };
const session: ClassSession = { id: 12, batchId: 7, batchNumber: batch.batchNumber, courseName: 'Cookery', branchId: 1,
  branchName: 'IHM Hotel School', sessionDate: '2026-10-05', startTime: '09:00:00', endTime: '13:00:00',
  lecturerUserId: null, lecturerName: null, topic: 'Kitchen safety', classroom: 'Kitchen 1', status: 'SCHEDULED',
  cancellationReason: null, originalSessionId: null, remarks: null, attendanceSubmittedAt: null,
  createdAt: '', updatedAt: '', version: 2, sourceScheduleId: null, generationDate: null, reschedulingReason: null };
const page = (rows: ClassSession[], total = rows.length) => ({ content: rows, page: 0, size: 100, totalElements: total, totalPages: Math.ceil(total / 100) });

describe('SessionListComponent', () => {
  let fixture: ComponentFixture<SessionListComponent>;
  let component: SessionListComponent;
  let api: { list: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn>; cancel: ReturnType<typeof vi.fn> };
  let batches: { get: ReturnType<typeof vi.fn>; lecturers: ReturnType<typeof vi.fn>; list: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };
  let route: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date(2026, 9, 5, 10));
    localStorage.clear();
    api = { list: vi.fn(() => of(page([session]))), get: vi.fn(() => of(session)), cancel: vi.fn(() => of({ ...session, status: 'CANCELLED' })) };
    batches = { get: vi.fn(() => of(batch)), lecturers: vi.fn(() => of([])), list: vi.fn(() => of(page([]))) };
    dialog = { open: vi.fn(() => ({ afterClosed: () => of(undefined) })) };
    route = new BehaviorSubject(convertToParamMap({ batchId: 7 }));
    await TestBed.configureTestingModule({ imports: [SessionListComponent], providers: [provideRouter([]), provideNativeDateAdapter(),
      { provide: ActivatedRoute, useValue: { queryParamMap: route } }, { provide: SessionService, useValue: api },
      { provide: BatchService, useValue: batches }, { provide: MatDialog, useValue: dialog },
      { provide: NotificationService, useValue: { success: vi.fn() } },
    ] }).compileComponents();
    TestBed.inject(ActiveBranchService).configure([{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      { id: 2, code: 'IHM-TEST', name: 'QA Branch' }]);
  });
  afterEach(() => { fixture?.destroy(); localStorage.clear(); vi.useRealTimers(); });
  async function start() { fixture = TestBed.createComponent(SessionListComponent); component = fixture.componentInstance; fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges(); }

  it('opens scoped calendar with a complete 42-day range and batch entry point', async () => {
    await start();
    expect(component.days().length).toBe(42);
    expect(component.days()[0].getDay()).toBe(1);
    expect(api.list).toHaveBeenCalledWith(expect.objectContaining({ batchId: 7, dateFrom: '2026-09-28', dateTo: '2026-11-08', size: 100 }));
    expect(fixture.nativeElement.querySelectorAll('.calendar-session').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('2026/CK01');
    expect(fixture.nativeElement.querySelector('.sessions-page button')?.textContent).toContain('Add session');
  });

  it('uses server pagination and selected date filters in list view', async () => {
    await start(); component.setView('list'); component.dateFrom.setValue(new Date(2026, 9, 1));
    component.dateTo.setValue(new Date(2026, 9, 31)); component.applyFilters();
    component.changePage({ pageIndex: 2, pageSize: 50, length: 200 });
    expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ batchId: 7, dateFrom: '2026-10-01', dateTo: '2026-10-31', page: 2, size: 50 }));
    fixture.detectChanges(); expect(fixture.nativeElement.querySelector('table[mat-table]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.ihm-mobile-card')).toBeTruthy();
  });

  it('refuses to show a partial month when the bounded query is too large', async () => {
    api.list.mockReturnValue(of(page([session], 501))); await start();
    expect(component.tooMany()).toBe(true); expect(component.rows()).toEqual([]);
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('More than 500 sessions');
  });

  it('drops the scoped batch when the active branch changes', async () => {
    await start();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    TestBed.inject(ActiveBranchService).selectBranch(2); fixture.detectChanges();
    expect(navigate).toHaveBeenCalledWith(['/sessions'], { replaceUrl: true });
  });

  it('shows the original and replacement after rescheduling', async () => {
    await start(); const replacement = { ...session, id: 13, originalSessionId: 12, sessionDate: '2026-10-06' };
    dialog.open.mockReturnValue({ afterClosed: () => of({ original: { ...session, status: 'RESCHEDULED' }, replacement }) });
    component.open(session); component.edit('reschedule', session); fixture.detectChanges();
    expect(component.selected()?.id).toBe(13);
    expect(fixture.nativeElement.textContent).toContain('Session #12 was rescheduled to #13');
    expect(fixture.nativeElement.textContent).toContain('Rescheduled from');
  });

  it('reloads latest details after a stale-version conflict', async () => {
    await start(); api.get.mockReturnValue(of({ ...session, version: 3 }));
    dialog.open.mockReturnValue({ afterClosed: () => of({ conflict: true }) });
    component.edit('edit', session);
    expect(component.selected()?.version).toBe(3);
    expect(component.actionError()).toContain('latest details have been reloaded');
  });

  it('requires confirmation and sends the current version when cancelling', async () => {
    await start(); dialog.open.mockReturnValue({ afterClosed: () => of({ confirmed: true, reason: 'Public holiday' }) });
    component.cancel(session);
    expect(api.cancel).toHaveBeenCalledWith(12, 'Public holiday', 2);
    expect(component.selected()?.status).toBe('CANCELLED');
    api.cancel.mockReturnValue(throwError(() => ({ status: 409 })));
    component.cancel(session); expect(component.actionError()).toContain('latest details have been reloaded');
  });

  it('clears a lecturer session and scoped batch after assignment access is revoked', async () => {
    await start(); component.open(session); expect(component.selected()?.id).toBe(12);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    dialog.open.mockReturnValue({ afterClosed: () => of({ accessDenied: true }) });
    component.edit('edit', session); fixture.detectChanges();
    expect(component.selected()).toBeNull(); expect(component.batch()).toBeNull();
    expect(component.canAdd()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/sessions'], { replaceUrl: true });
    expect(component.actionError()).toContain('no longer have access');
  });

  it('clears details when the session detail API returns forbidden', async () => {
    await start(); component.open(session); expect(component.selected()).not.toBeNull();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    api.get.mockReturnValue(throwError(() => ({ status: 403, error: { message: 'Access denied' } })));
    component.open(session); fixture.detectChanges();
    expect(component.selected()).toBeNull(); expect(component.canAdd()).toBe(false);
  });
});
